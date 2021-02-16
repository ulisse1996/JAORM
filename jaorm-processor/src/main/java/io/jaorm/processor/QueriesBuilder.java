package io.jaorm.processor;

import com.squareup.javapoet.*;
import io.jaorm.Arguments;
import io.jaorm.BaseDao;
import io.jaorm.spi.QueryRunner;
import io.jaorm.cache.Cacheable;
import io.jaorm.spi.DelegatesService;
import io.jaorm.entity.sql.SqlParameter;
import io.jaorm.processor.annotation.Dao;
import io.jaorm.processor.annotation.Query;
import io.jaorm.processor.exception.ProcessorException;
import io.jaorm.processor.strategy.QueryStrategy;
import io.jaorm.processor.util.MethodUtils;
import io.jaorm.processor.util.ReturnTypeDefinition;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QueriesBuilder {

    private static final String REQUIRED_NOT_NULL_STATEMENT = "$T.requireNonNull(arg0, $S)";
    private static final String ENTITY_CAN_T_BE_NULL = "Entity can't be null !";
    private final Set<TypeElement> types;
    private final ProcessingEnvironment processingEnvironment;

    public QueriesBuilder(ProcessingEnvironment processingEnvironment, Set<TypeElement> types) {
        this.types = types;
        this.processingEnvironment = processingEnvironment;
    }

    public void process() {
        for (TypeElement query : types) {
            Set<MethodSpec> methods = new HashSet<>();
            for (Element ele : query.getEnclosedElements()) {
                if (ele.getAnnotation(Query.class) != null) {
                    ExecutableElement executableElement = (ExecutableElement) ele;
                    MethodSpec methodSpec = buildImpl(executableElement);
                    methods.add(methodSpec);
                }
            }

            List<AnnotationSpec> annotations = getExtraAnnotations(query);

            if (isBaseDao(query)) {
                methods.addAll(buildBaseDao(query));
            }

            String packageName = getPackage(query);
            TypeSpec build = TypeSpec.classBuilder(query.getSimpleName() + "Impl")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotations(annotations)
                    .addSuperinterface(query.asType())
                    .addMethods(methods)
                    .build();
            try {
                JavaFile file = JavaFile.builder(packageName, build)
                        .indent("    ")
                        .skipJavaLangImports(true)
                        .build();
                file.writeTo(processingEnvironment.getFiler());
            } catch (IOException ex) {
                throw new ProcessorException(ex);
            }
        }
    }

    private List<AnnotationSpec> getExtraAnnotations(TypeElement query) {
        return query.getAnnotationMirrors()
                .stream()
                .map(AnnotationSpec::get)
                .filter(an -> !an.type.equals(TypeName.get(Dao.class)))
                .collect(Collectors.toList());
    }

    private String getPackage(TypeElement entity) {
        return ClassName.get(entity).packageName();
    }

    private Collection<MethodSpec> buildBaseDao(TypeElement query) {
        String realClass = getBaseDaoGeneric(query);
        ClassName className = ClassName.bestGuess(realClass);
        MethodSpec read = resolveParameter(MethodSpec.overriding(MethodUtils.getMethod(processingEnvironment, "read", BaseDao.class))
                .returns(className)
                .addStatement(REQUIRED_NOT_NULL_STATEMENT, Objects.class, ENTITY_CAN_T_BE_NULL)
                .addStatement("$T arguments = $T.getInstance().asWhere($L)", Arguments.class, DelegatesService.class, "arg0")
                .addStatement("return $T.getCached($T.class, arguments, () -> $T.getInstance($T.class).read($T.class, $T.getInstance().getSql($T.class), argumentsAsParameters($T.getInstance().asWhere($L).getValues())))",
                        Cacheable.class, className, QueryRunner.class, className, className, DelegatesService.class, className, DelegatesService.class, "arg0"), realClass);
        MethodSpec readOpt = resolveParameter(MethodSpec.overriding(MethodUtils.getMethod(processingEnvironment, "readOpt", BaseDao.class))
                .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), className))
                .addStatement(REQUIRED_NOT_NULL_STATEMENT, Objects.class, ENTITY_CAN_T_BE_NULL)
                .addStatement("$T arguments = $T.getInstance().asWhere($L)", Arguments.class, DelegatesService.class, "arg0")
                .addStatement("return $T.getCachedOpt($T.class, arguments, () -> $T.getInstance($T.class).readOpt($T.class, $T.getInstance().getSql($T.class), argumentsAsParameters($T.getInstance().asWhere($L).getValues())))",
                        Cacheable.class, className, QueryRunner.class, className, className, DelegatesService.class, className, DelegatesService.class, "arg0"), realClass);
        MethodSpec readAll = resolveParameter(MethodSpec.overriding(MethodUtils.getMethod(processingEnvironment, "readAll", BaseDao.class))
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), className))
                .addStatement("return $T.getCachedAll($T.class, () -> $T.getInstance($T.class).readAll($T.class, $T.getInstance().getSimpleSql($T.class), $T.emptyList()))",
                        Cacheable.class, className, QueryRunner.class, className, className, DelegatesService.class, className, Collections.class), realClass);
        return Stream.of(read, readOpt, readAll).collect(Collectors.toList());
    }

    private MethodSpec resolveParameter(MethodSpec.Builder builder, String realClass) {
        List<ParameterSpec> specs = builder.parameters;
        for (int i = 0; i < specs.size(); i++) {
            ParameterSpec spec = specs.get(i);
            if (!spec.type.equals(ClassName.bestGuess(realClass))) {
                ParameterSpec newSpec = ParameterSpec.builder(ClassName.bestGuess(realClass), spec.name)
                        .build();
                specs.set(i, newSpec);
            }
        }

        return builder.build();
    }

    public static String getBaseDaoGeneric(TypeElement query) {
        for (TypeMirror typeMirror : query.getInterfaces()) {
            if (typeMirror.toString().contains(BaseDao.class.getName())) {
                return typeMirror.toString().replace(BaseDao.class.getName(), "")
                        .replace("<", "")
                        .replace(">", "");
            }
        }

        throw new ProcessorException("Can't find generic type of BaseDao");
    }

    public static boolean isBaseDao(TypeElement query) {
        if (!query.getInterfaces().isEmpty()) {
            for (TypeMirror typeMirror : query.getInterfaces()) {
                if (typeMirror.toString().contains(BaseDao.class.getName())) {
                    return true;
                }
            }
        }

        return false;
    }

    private MethodSpec buildImpl(ExecutableElement executableElement) {
        Query query = executableElement.getAnnotation(Query.class);
        String sql = query.sql();
        MethodSpec.Builder builder = MethodSpec.overriding(executableElement)
                .addStatement("$T params = new $T<>()",
                        ParameterizedTypeName.get(List.class, SqlParameter.class), ArrayList.class);
        for (QueryStrategy queryStrategy : QueryStrategy.values()) {
            if (queryStrategy.isValid(sql)) {
                int paramNumber = queryStrategy.getParamNumber(sql);
                if (paramNumber != executableElement.getParameters().size()) {
                    throw new ProcessorException("Mismatch between parameters and query parameters for method " + executableElement);
                }
                List<CodeBlock> statements = queryStrategy.extract(this.processingEnvironment, sql, executableElement);
                statements.forEach(builder::addCode);
                sql = queryStrategy.replaceQuery(sql);
                Map.Entry<String, Object[]> checked = checkType(sql, executableElement);
                builder.addStatement(checked.getKey(), checked.getValue());
                return builder.build();
            }
        }

        throw new ProcessorException("Can't find query strategy");
    }

    private Map.Entry<String, Object[]> checkType(String sql, ExecutableElement method) {
        if (sql.toUpperCase().startsWith("SELECT")) {
            return checkSelect(sql, method);
        } else if (sql.toUpperCase().startsWith("DELETE")) {
            assertVoid(method);
            return new AbstractMap.SimpleImmutableEntry<>("$T.getSimple().delete($S, params)",
                    new Object[] {QueryRunner.class, sql});
        } else if (sql.toUpperCase().startsWith("UPDATE")) {
            assertVoid(method);
            return new AbstractMap.SimpleImmutableEntry<>("$T.getSimple().update($S, params)",
                    new Object[] {QueryRunner.class, sql});
        }

        throw new ProcessorException(String.format("Operation not supported for sql [%s] in method [%s]", sql, method));
    }

    private void assertVoid(ExecutableElement method) {
        if (!method.getReturnType().getKind().equals(TypeKind.VOID)) {
            throw new ProcessorException("Can't use Delete or Update statement with a non-void method");
        }
    }

    private Map.Entry<String, Object[]> checkSelect(String sql, ExecutableElement method) {
        if (method.getReturnType().getKind().equals(TypeKind.VOID)) {
            throw new ProcessorException(String.format("Select of method %s need a return type !", method));
        } else {
            ReturnTypeDefinition definition = new ReturnTypeDefinition(processingEnvironment, method.getReturnType());
            Object[] stmParams = {
                    QueryRunner.class, definition.getRealClass(), definition.getRealClass(), sql
            };
            if (definition.isOptional()) {
                return new AbstractMap.SimpleImmutableEntry<>(
                        "return $T.getInstance($T.class).readOpt($T.class, $S, params)",
                        stmParams
                );
            } else if (definition.isCollection()) {
                return new AbstractMap.SimpleImmutableEntry<>(
                        "return $T.getInstance($T.class).readAll($T.class, $S, params)",
                        stmParams
                );
            } else {
                return new AbstractMap.SimpleImmutableEntry<>(
                        "return $T.getInstance($T.class).read($T.class, $S, params)",
                        stmParams
                );
            }
        }
    }
}
