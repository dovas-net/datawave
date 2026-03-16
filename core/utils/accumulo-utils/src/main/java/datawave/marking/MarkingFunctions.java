package datawave.marking;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.accumulo.access.Access;
import org.apache.accumulo.access.AccessExpression;
import org.apache.accumulo.access.ParsedAccessExpression;
import org.apache.accumulo.access.examples.ParseExamples;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Accumulo marks all data with a columnVisibility that declares and controls access. MarkingFunctions provide a pattern for mapping a user's preferred means of
 * declaring access controls with the Accumulo columnVisibility pattern. As an example, James Bond might use MarkingFunctions to translate a the
 * columnVisibility FYEO into the song "For Your Eyes Only" A hospital might use a columnVisibility like 'PPI' but prefer to mark data input and results with a
 * human-readable marking like "Patient Privileged Information"
 */

public interface MarkingFunctions<T extends Markings<?>> {

    AccessExpression combine(Collection<AccessExpression> columnVisibilities) throws MarkingFunctions.Exception;

    T combine(T... markings) throws MarkingFunctions.Exception;

    AccessExpression translateToColumnVisibility(T markings) throws MarkingFunctions.Exception;

    T translateFromColumnVisibility(AccessExpression columnVisibility) throws MarkingFunctions.Exception;

    class Exception extends java.lang.Exception {

        public Exception() {
            super();
        }

        public Exception(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }

        public Exception(String message, Throwable cause) {
            super(message, cause);
        }

        public Exception(String message) {
            super(message);
        }

        public Exception(Throwable cause) {
            super(cause);
        }
    }

    class Default implements MarkingFunctions<AccessExpressionMarkings> {
        public static final Access ACCESS = Access.builder().build();

        @Override
        public AccessExpression combine(Collection<AccessExpression> expressions) {

            // filter out any empty expressions and concatenate with '&'
            // @formatter:off
            ParsedAccessExpression expression = ACCESS.newParsedExpression(expressions
                                                .stream()
                                                .map(AccessExpression::getExpression)
                                                .filter(str -> !str.isEmpty())
                                                .map(str -> "(" + str + ")")
                                                .collect(Collectors.joining("&"))
                                                );
            // @formatter:on

            // normalize the Parsed Expression and then return a copy without the parse tree
            // FIXME: Using a beta of the accumulo-access API so temporarily using the normalize function from the example code
            return ACCESS.newExpression(ParseExamples.normalize(expression).expression);
        }

        @Override
        public final AccessExpressionMarkings combine(AccessExpressionMarkings... markings) {
            // translate COLUMN_VISIBILITY values to ColumnVisibility, combine them and
            // return translated back to Map
            return AccessExpressionMarkings.builder().accessExpression(combine(Arrays.stream(markings).map(Markings::getMarkings).collect(Collectors.toSet())))
                            .build();
        }

        @Override
        public AccessExpression translateToColumnVisibility(AccessExpressionMarkings markings) {
            return markings.getMarkings();
        }

        @Override
        public AccessExpressionMarkings translateFromColumnVisibility(AccessExpression expression) {
            return AccessExpressionMarkings.builder().accessExpression(expression).build();
        }

    }

    class Util {

        public static Object populate(Object obj, Map<String,String> source) {
            try {
                BeanUtils.populate(obj, source);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Error populating object: " + obj.getClass().getName(), e);
            }
            return obj;
        }
    }

    class Encoding {
        static private Logger log = LoggerFactory.getLogger(Encoding.class);

        /**
         * Turn a set of markings into a serializable string
         *
         * @param markings
         *            the markings map to convert to a string
         * @return a serialized String version of {@code markings}
         */
        public static String toString(Map<String,String> markings) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return objectMapper.writeValueAsString(markings);
            } catch (JsonProcessingException e) {
                log.error("could not serialize " + markings);
                return "";
            }
        }

        /**
         * Turn a serialized set of markings into a map
         *
         * @param encodedMarkings
         *            the serialized String markings to convert back to a markings Map
         * @return a {@link Map} of the de-serialized markings from {@code encodedMarkings}
         */
        public static Map<String,String> fromString(String encodedMarkings) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                // noinspection unchecked
                return objectMapper.readValue(encodedMarkings, Map.class);
            } catch (IOException e) {
                log.error("could not deserialize " + encodedMarkings);
                return Collections.emptyMap();
            }
        }
    }

    /**
     * this Factory for MarkingFunctions is designed to be used on the tservers, where there is a vfs-classloader
     */
    class Factory {
        public static final Logger log = LoggerFactory.getLogger(Factory.class);

        private static MarkingFunctions markingFunctions;

        public static synchronized MarkingFunctions createMarkingFunctions() {
            if (markingFunctions != null)
                return markingFunctions;
            ClassLoader thisClassLoader = Factory.class.getClassLoader();

            // ignore calls to close as this blows away the cache manager
            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext();
            try {
                // To prevent failure when this is run on the tservers:
                // The VFS ClassLoader has been created and has been made the current thread's context classloader, but its resource paths are empty at this
                // time.
                // The spring ApplicationContext will prefer the current thread's context classloader, so the spring context would fail to find
                // any classes or context files to load.
                // Instead, set the classloader on the ApplicationContext to be the one that is loading this class.
                // It is a VFSClassLoader that has the accumulo lib/ext jars set as its resources.
                // After setting the classloader, then set the config locations and refresh the context.
                context.setClassLoader(thisClassLoader);
                context.setConfigLocations("classpath*:/MarkingFunctionsContext.xml");
                context.refresh();
                markingFunctions = context.getBean("markingFunctions", MarkingFunctions.class);
            } catch (Throwable t) {
                // got here because the VFSClassLoader on the tservers does not implement findResources
                // none of the spring wiring will work.
                log.warn("Could not load spring context files. got " + t);
            }

            return markingFunctions;
        }
    }
}
