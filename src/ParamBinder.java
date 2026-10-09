package src;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import src.annotation.DateFormat;
import src.annotation.Numeric;
import src.annotation.Param;
import src.annotation.ParamObject;
import src.annotation.Range;
import src.annotation.Required;

// Sprint 7 : @Param simple + conversion String -> int/double/boolean/Date
// Sprint 7b : @ParamObject(prefix) + validation (@Required, @Numeric, @Range, @DateFormat)
public class ParamBinder {

    public static Object[] resolveMethodArgs(Method method, HttpServletRequest req, HttpServletResponse res)
            throws Exception {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            Class<?> type = p.getType();

            // 1) Objets servlet injectés directement
            if (type.isAssignableFrom(HttpServletRequest.class)) {
                args[i] = req;
                continue;
            }
            if (type.isAssignableFrom(HttpServletResponse.class)) {
                args[i] = res;
                continue;
            }
            if (type.isAssignableFrom(HttpSession.class)) {
                args[i] = req.getSession();
                continue;
            }

            // 2) Refus explicite de @RequestParam (Spring) -> ETU002391
            for (Annotation a : p.getAnnotations()) {
                String simple = a.annotationType().getSimpleName();
                if (simple.equals("RequestParam")) {
                    throw new BindingException(
                        "ETU002391 : @RequestParam non supporte par le framework. "
                        + "Utilisez @Param(name=\"...\") (Sprint 7) ou @ParamObject(name=\"...\") (Sprint 7b) "
                        + "sur " + method.getDeclaringClass().getSimpleName() + "." + method.getName()
                        + " param[" + p.getName() + "].");
                }
            }

            // 3) @Param : binding simple
            Param paramAnn = p.getAnnotation(Param.class);
            if (paramAnn != null) {
                String name = !paramAnn.name().isBlank() ? paramAnn.name() : paramAnn.value();
                if (name.isBlank()) {
                    // fallback : nom du paramètre (nécessite -parameters à la compilation)
                    name = p.getName();
                }
                String raw = req.getParameter(name);
                // getParameterValues pour debug ? on reste simple
                try {
                    args[i] = convert(raw, type, name, null);
                } catch (BindingException be) {
                    throw be;
                } catch (Exception e) {
                    throw new BindingException(
                        "Echec de conversion @Param(name=\"" + name + "\")=\"" + raw
                        + "\" vers " + type.getSimpleName() + " : " + e.getMessage(), e);
                }
                continue;
            }

            // 4) @ParamObject : binding objet via prefix.champ
            ParamObject poAnn = p.getAnnotation(ParamObject.class);
            if (poAnn != null) {
                String prefix = !poAnn.name().isBlank() ? poAnn.name() : poAnn.value();
                if (prefix.isBlank()) {
                    prefix = decapitalize(type.getSimpleName());
                }
                Object obj = bindObject(type, prefix, req);
                validateObject(obj);
                args[i] = obj;
                continue;
            }

            // 5) Aucune annotation supportée
            throw new BindingException(
                "Parametre non annote sur " + method.getDeclaringClass().getSimpleName()
                + "." + method.getName() + "() param[" + i + " " + type.getSimpleName() + " " + p.getName() + "] : "
                + "ajoutez @Param(name=\"...\") (Sprint 7), @ParamObject(name=\"...\") (Sprint 7b), "
                + "ou HttpServletRequest/Response/Session.");
        }
        return args;
    }

    // ---------- Binding objet ----------
    private static Object bindObject(Class<?> clazz, String prefix, HttpServletRequest req) throws Exception {
        Object instance;
        try {
            instance = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new BindingException(
                "Impossible d'instancier " + clazz.getSimpleName()
                + " (constructeur sans argument requis) : " + e.getMessage(), e);
        }

        for (Field f : getAllFields(clazz)) {
            String keyPrefixed = prefix + "." + f.getName();
            String raw = req.getParameter(keyPrefixed);
            // tolérance : accepte aussi sans préfixe (ex : ?nom=... au lieu de ?etudiant.nom=...)
            if (raw == null) {
                raw = req.getParameter(f.getName());
            }
            if (raw == null) {
                continue; // champ absent -> reste null / 0, @Required s'en chargera
            }
            String pattern = null;
            DateFormat df = f.getAnnotation(DateFormat.class);
            if (df != null) {
                pattern = df.pattern();
            }
            Object converted;
            try {
                converted = convert(raw, f.getType(), keyPrefixed, pattern);
            } catch (Exception e) {
                throw new BindingException(
                    "Echec de conversion " + keyPrefixed + "=\"" + raw + "\" vers "
                    + f.getType().getSimpleName() + " : " + e.getMessage(), e);
            }
            setFieldValue(instance, f, converted);
        }
        return instance;
    }

    private static void setFieldValue(Object instance, Field field, Object value) throws Exception {
        // essaie le setter setXxx() d'abord
        String setter = "set" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        try {
            Method m = null;
            for (Method cand : instance.getClass().getMethods()) {
                if (cand.getName().equals(setter) && cand.getParameterCount() == 1) {
                    // compatible (y compris primitive/wrapper)
                    if (cand.getParameterTypes()[0].isAssignableFrom(field.getType())
                            || isPrimitiveCompatible(cand.getParameterTypes()[0], field.getType())
                            || (value != null && cand.getParameterTypes()[0].isAssignableFrom(value.getClass()))) {
                        m = cand;
                        break;
                    }
                }
            }
            if (m != null) {
                m.invoke(instance, value);
                return;
            }
        } catch (BindingException be) {
            throw be;
        } catch (Exception ignored) {
            // fallback : accès direct
        }
        field.setAccessible(true);
        field.set(instance, value);
    }

    private static boolean isPrimitiveCompatible(Class<?> a, Class<?> b) {
        return box(a).isAssignableFrom(box(b)) || box(b).isAssignableFrom(box(a));
    }

    private static Class<?> box(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == double.class) return Double.class;
        if (c == float.class) return Float.class;
        if (c == boolean.class) return Boolean.class;
        if (c == short.class) return Short.class;
        if (c == byte.class) return Byte.class;
        if (c == char.class) return Character.class;
        return c;
    }

    // ---------- Validation Sprint 7b ----------
    private static void validateObject(Object obj) {
        List<String> errors = new ArrayList<>();
        for (Field f : getAllFields(obj.getClass())) {
            f.setAccessible(true);
            Object value;
            try {
                value = f.get(obj);
            } catch (IllegalAccessException e) {
                continue;
            }

            Required req = f.getAnnotation(Required.class);
            if (req != null) {
                if (value == null || (value instanceof String s && s.isBlank())) {
                    errors.add(f.getName() + " : " + req.message() + " (@Required)");
                }
            }

            Numeric num = f.getAnnotation(Numeric.class);
            if (num != null && value != null) {
                if (value instanceof Number) {
                    // ok
                } else if (value instanceof String s) {
                    if (!s.isBlank() && !s.trim().matches("[+-]?\\d+(\\.\\d+)?")) {
                        errors.add(f.getName() + " : " + num.message() + " (@Numeric, valeur=\"" + value + "\")");
                    }
                } else {
                    errors.add(f.getName() + " : " + num.message() + " (@Numeric)");
                }
            }

            Range range = f.getAnnotation(Range.class);
            if (range != null && value != null) {
                Double d = null;
                if (value instanceof Number n) {
                    d = n.doubleValue();
                } else if (value instanceof String s && !s.isBlank()) {
                    try {
                        d = Double.parseDouble(s.trim());
                    } catch (NumberFormatException e) {
                        errors.add(f.getName() + " : non numerique pour @Range (valeur=\"" + value + "\")");
                        continue;
                    }
                }
                if (d != null) {
                    if (d < range.min() || d > range.max()) {
                        errors.add(f.getName() + " : " + range.message()
                            + " (@Range min=" + range.min() + " max=" + range.max() + ", valeur=" + value + ")");
                    }
                }
            }

            DateFormat df = f.getAnnotation(DateFormat.class);
            if (df != null && value != null) {
                // si le champ est String, vérifie que le format est parsable
                if (value instanceof String s && !s.isBlank()) {
                    if (!tryParseDate(s.trim(), df.pattern())) {
                        errors.add(f.getName() + " : " + df.message()
                            + " (@DateFormat pattern=\"" + df.pattern() + "\", valeur=\"" + value + "\")");
                    }
                }
                // si c'est déjà une Date convertie, le parsing a déjà réussi -> ok
            }
        }
        if (!errors.isEmpty()) {
            throw new BindingException("Echec validation (" + obj.getClass().getSimpleName() + ") : "
                + String.join(" | ", errors));
        }
    }

    private static boolean tryParseDate(String value, String pattern) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);
            // essaie LocalDate, puis LocalDateTime, puis java.util.Date
            try {
                LocalDate.parse(value, fmt);
                return true;
            } catch (Exception ignored) {}
            try {
                LocalDateTime.parse(value, fmt);
                return true;
            } catch (Exception ignored) {}
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            sdf.setLenient(false);
            sdf.parse(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------- Conversion String -> type ----------
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object convert(String raw, Class<?> target, String paramName, String datePattern) throws Exception {
        String v = raw == null ? null : raw.trim();

        // null / vide
        if (v == null || v.isEmpty()) {
            if (target == String.class) return null;
            if (target.isPrimitive()) {
                if (target == boolean.class) return false;
                if (target == char.class) return '\0';
                if (target == int.class) throw new BindingException("Parametre manquant : \"" + paramName + "\" (int requis)");
                if (target == long.class) throw new BindingException("Parametre manquant : \"" + paramName + "\" (long requis)");
                if (target == double.class) throw new BindingException("Parametre manquant : \"" + paramName + "\" (double requis)");
                if (target == float.class) throw new BindingException("Parametre manquant : \"" + paramName + "\" (float requis)");
                if (target == short.class) throw new BindingException("Parametre manquant : \"" + paramName + "\" (short requis)");
                if (target == byte.class) throw new BindingException("Parametre manquant : \"" + paramName + "\" (byte requis)");
            }
            return null;
        }

        if (target == String.class) return raw;
        if (target == int.class || target == Integer.class) return Integer.parseInt(v);
        if (target == long.class || target == Long.class) return Long.parseLong(v);
        if (target == double.class || target == Double.class) return Double.parseDouble(v);
        if (target == float.class || target == Float.class) return Float.parseFloat(v);
        if (target == short.class || target == Short.class) return Short.parseShort(v);
        if (target == byte.class || target == Byte.class) return Byte.parseByte(v);
        if (target == boolean.class || target == Boolean.class) return parseBoolean(v);
        if (target == char.class || target == Character.class) return v.charAt(0);

        if (target.isEnum()) {
            return Enum.valueOf((Class<Enum>) target, v);
        }

        // java.sql.Date (attendu Sprint 7 : inscription=2026-09-30)
        if (target == java.sql.Date.class) {
            if (datePattern != null && !datePattern.equals("yyyy-MM-dd")) {
                SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
                sdf.setLenient(false);
                java.util.Date d = sdf.parse(v);
                return new java.sql.Date(d.getTime());
            }
            return java.sql.Date.valueOf(v); // format yyyy-[m]m-[d]d
        }
        if (target == java.util.Date.class) {
            String pat = datePattern != null ? datePattern : "yyyy-MM-dd";
            SimpleDateFormat sdf = new SimpleDateFormat(pat);
            sdf.setLenient(false);
            try {
                return sdf.parse(v);
            } catch (Exception e) {
                // fallback : tente yyyy-MM-dd'T'HH:mm et dd/MM/yyyy
                for (String p : new String[]{ "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd HH:mm:ss", "dd/MM/yyyy" }) {
                    try {
                        SimpleDateFormat s2 = new SimpleDateFormat(p);
                        s2.setLenient(false);
                        return s2.parse(v);
                    } catch (Exception ignored) {}
                }
                throw e;
            }
        }
        if (target == java.sql.Timestamp.class) {
            try {
                return java.sql.Timestamp.valueOf(v);
            } catch (Exception e) {
                java.util.Date d = (java.util.Date) convert(v, java.util.Date.class, paramName, datePattern);
                return new java.sql.Timestamp(d.getTime());
            }
        }
        if (target == LocalDate.class) {
            String pat = datePattern != null ? datePattern : "yyyy-MM-dd";
            return LocalDate.parse(v, DateTimeFormatter.ofPattern(pat));
        }
        if (target == LocalDateTime.class) {
            String pat = datePattern != null ? datePattern : "yyyy-MM-dd'T'HH:mm";
            try {
                return LocalDateTime.parse(v, DateTimeFormatter.ofPattern(pat));
            } catch (Exception e) {
                return LocalDateTime.parse(v); // ISO
            }
        }
        if (target == LocalTime.class) {
            return LocalTime.parse(v);
        }

        throw new BindingException("Type non supporte pour le binding : " + target.getName()
            + " (param=\"" + paramName + "\"). Types supportes : String, int/Integer, long, double, float, short, byte, boolean, char, Enum, java.sql.Date, java.util.Date, LocalDate.");
    }

    private static boolean parseBoolean(String v) {
        String s = v.trim().toLowerCase();
        if (s.equals("true") || s.equals("1") || s.equals("on") || s.equals("oui") || s.equals("yes")) return true;
        if (s.equals("false") || s.equals("0") || s.equals("off") || s.equals("non") || s.equals("no") || s.equals("")) return false;
        return Boolean.parseBoolean(s);
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> list = new ArrayList<>();
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isSynthetic()) continue;
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                list.add(f);
            }
            c = c.getSuperclass();
        }
        return list;
    }

    private static String decapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
