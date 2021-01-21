package io.jaorm.processor;

import com.squareup.javapoet.*;
import io.jaorm.QueryRunner;
import io.jaorm.entity.*;
import io.jaorm.entity.sql.SqlAccessor;
import io.jaorm.entity.sql.SqlParameter;
import io.jaorm.processor.annotation.*;
import io.jaorm.processor.exception.ProcessorException;
import io.jaorm.processor.util.Accessor;
import io.jaorm.processor.util.MethodUtils;
import io.jaorm.processor.util.ParameterConverter;
import io.jaorm.processor.util.ReturnTypeDefinition;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.NoType;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntitiesBuilder {

    private static final String COL_NAME = "colName";
    private final Set<TypeElement> entities;
    private final ProcessingEnvironment processingEnv;

    public EntitiesBuilder(ProcessingEnvironment processingEnv, Set<TypeElement> entities) {
        this.entities = entities;
        this.processingEnv = processingEnv;
    }

    public void process() {
        for (TypeElement entity : entities) {
            String packageName = getPackage(entity);
            String delegate = entity.getSimpleName() + "Delegate";
            TypeSpec spec = build(entity, delegate);
            try {
                JavaFile file = JavaFile.builder(packageName, spec)
                        .indent("    ")
                        .skipJavaLangImports(true)
                        .build();
                file.writeTo(processingEnv.getFiler());
            } catch (IOException ex) {
                throw new ProcessorException(ex);
            }
        }
    }

    private String getPackage(TypeElement entity) {
        return ClassName.get(entity).packageName();
    }

    private TypeSpec build(TypeElement entity, String delegateName) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(ClassName.get(getPackage(entity), delegateName))
                .addModifiers(Modifier.PUBLIC)
                .superclass(entity.asType())
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(EntityDelegate.class), ClassName.get(entity)));
        TypeSpec columns = buildColumnEnum(entity);
        builder.addType(columns);
        builder.addField(ClassName.get(entity), "entity", Modifier.PRIVATE);
        builder.addField(addTableName(entity));
        builder.addField(addBaseSql(entity));
        builder.addField(addKeysWhere());
        builder.addField(addInsertSql());
        builder.addMethods(buildDelegation(entity));
        builder.addMethods(buildOverrideEntity(entity));
        return builder.build();
    }

    private FieldSpec addInsertSql() {
        return FieldSpec.builder(String.class, "INSERT_SQL", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("Column.getInsertSql()")
                .build();
    }

    private FieldSpec addKeysWhere() {
        return FieldSpec.builder(String.class, "KEYS_WHERE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("Column.getKeyWheres()")
                .build();
    }

    private FieldSpec addBaseSql(TypeElement entity) {
        String select = "SELECT ";
        String from = " FROM " + entity.getAnnotation(Table.class).name();
        return FieldSpec.builder(String.class, "BASE_SQL", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S + Column.getSelectables() + $S", select, from)
                .build();
    }

    private FieldSpec addTableName(TypeElement entity) {
        return FieldSpec.builder(String.class, "TABLE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", entity.getAnnotation(Table.class).name())
                .build();
    }

    private Iterable<MethodSpec> buildOverrideEntity(TypeElement entity) {
        MethodSpec supplierEntity = MethodSpec.overriding(MethodUtils.getMethod(processingEnv,"getEntityInstance", EntityDelegate.class))
                .returns(ParameterizedTypeName.get(ClassName.get(Supplier.class), ClassName.get(entity)))
                .addStatement("return $T::new", entity)
                .build();
        MethodSpec entityMapper = MethodSpec.overriding(MethodUtils.getMethod(processingEnv, "getEntityMapper", EntityDelegate.class))
                .returns(ParameterizedTypeName.get(ClassName.get(EntityMapper.class), ClassName.get(entity)))
                .addStatement("return Column.getEntityMapper()")
                .build();
        MethodSpec setEntity = MethodSpec.overriding(MethodUtils.getMethod(processingEnv, "setEntity", EntityDelegate.class))
                .addStatement("this.entity = toEntity($L)", extractParameterNames(MethodUtils.getMethod(processingEnv, "setEntity", EntityDelegate.class)))
                .build();
        MethodSpec setEntityObj = MethodSpec.methodBuilder("setFullEntity")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(entity), "entity")
                .addStatement("this.entity = entity")
                .build();
        MethodSpec baseSql = MethodSpec.overriding(MethodUtils.getMethod(processingEnv, "getBaseSql", EntityDelegate.class))
                .addStatement("return BASE_SQL")
                .build();
        MethodSpec keysWhere = MethodSpec.overriding(MethodUtils.getMethod(processingEnv, "getKeysWhere", EntityDelegate.class))
                .addStatement("return KEYS_WHERE")
                .build();
        MethodSpec insertSql = MethodSpec.overriding(MethodUtils.getMethod(processingEnv, "getInsertSql", EntityDelegate.class))
                .addStatement("return INSERT_SQL")
                .build();
        return Stream.of(supplierEntity, entityMapper, setEntity, setEntityObj, baseSql, keysWhere, insertSql)
                .collect(Collectors.toList());
    }

    private TypeSpec buildColumnEnum(TypeElement entity) {
        List<Element> columns = entity.getEnclosedElements()
                .stream()
                .filter(ele -> ele.getAnnotation(Column.class) != null)
                .collect(Collectors.toList());
        List<Accessor> accessors = asAccessors(processingEnv, columns);
        TypeSpec.Builder builder = TypeSpec.enumBuilder("Column");
        builder.addModifiers(Modifier.PUBLIC);
        builder.addField(getColumnGetterType(entity), "getter", Modifier.PRIVATE, Modifier.FINAL);
        builder.addField(getColumnSetterType(entity), "setter", Modifier.PRIVATE, Modifier.FINAL);
        builder.addField(getGenericClassType(), "type", Modifier.PRIVATE, Modifier.FINAL);
        builder.addField(String.class, COL_NAME, Modifier.PRIVATE, Modifier.FINAL);
        builder.addField(boolean.class, "key", Modifier.PRIVATE, Modifier.FINAL);
        builder.addField(getEntityMapperType(entity), "ENTITY_MAPPER", Modifier.PRIVATE, Modifier.STATIC);
        builder.addField(String.class, "SELECTABLES", Modifier.PRIVATE, Modifier.STATIC);
        builder.addField(String.class, "KEYS_WHERE", Modifier.PRIVATE, Modifier.STATIC);
        builder.addField(String.class, "INSERT_SQL", Modifier.PRIVATE, Modifier.STATIC);
        builder.addMethod(addColumnConstructor(entity));
        for (Accessor accessor : accessors) {
            builder.addEnumConstant(accessor.getName(), enumImpl(accessor, entity));
        }
        builder.addMethod(
                MethodSpec.methodBuilder("getGetter")
                .returns(ParameterizedTypeName.get(ClassName.get(ColumnGetter.class), ClassName.get(entity), WildcardTypeName.subtypeOf(Object.class)))
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return this.getter")
                .build()
        );
        builder.addMethod(
                MethodSpec.methodBuilder("getEntityMapper")
                .returns(getEntityMapperType(entity))
                .addModifiers(Modifier.STATIC, Modifier.SYNCHRONIZED)
                .addCode(buildEntityMapperCodeBlock())
                .build()
        );
        builder.addMethod(
                MethodSpec.methodBuilder("getSelectables")
                .returns(String.class)
                .addModifiers(Modifier.STATIC, Modifier.SYNCHRONIZED)
                .addCode(buildSelectablesCodeBlock())
                .build()
        );
        builder.addMethod(
                MethodSpec.methodBuilder("getKeyWheres")
                .returns(String.class)
                .addModifiers(Modifier.STATIC, Modifier.SYNCHRONIZED)
                .addCode(buildKeysWhereCodeBlock())
                .build()
        );
        builder.addMethod(
                MethodSpec.methodBuilder("getInsertSql")
                .returns(String.class)
                .addModifiers(Modifier.STATIC, Modifier.SYNCHRONIZED)
                .addCode(buildInsertSqlCodeBlock(entity.getAnnotation(Table.class).name()))
                .build()
        );
        builder.addMethod(
                MethodSpec.methodBuilder("findColumn")
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.bestGuess("Column"))
                .addParameter(String.class, COL_NAME)
                .addModifiers(Modifier.STATIC)
                .addCode(buildFindColumnCodeBlock())
                .build()
        );
        return builder.build();
    }

    private CodeBlock buildInsertSqlCodeBlock(String table) {
        return CodeBlock.builder()
                .beginControlFlow("if (INSERT_SQL == null)")
                .addStatement("$T builder = new $T()", StringBuilder.class, StringBuilder.class)
                .addStatement("builder.append($S)", "INSERT INTO " + table + " (")
                .beginControlFlow("for (Column column : values())")
                .addStatement("builder.append(column.colName).append($S)",",")
                .endControlFlow()
                .addStatement("String wildcards = Stream.of(values()).map(c -> $S).collect($T.joining($S))", "?", Collectors.class, ",")
                .addStatement("builder.append($S).append(wildcards).append($S)", ") VALUES (", ")")
                .addStatement("INSERT_SQL = builder.toString()")
                .endControlFlow()
                .addStatement("return INSERT_SQL")
                .build();
    }

    private CodeBlock buildFindColumnCodeBlock() {
        return CodeBlock.builder()
                .beginControlFlow("for (Column col : values())")
                .beginControlFlow("if (col.colName.equals(colName))")
                .addStatement("return col")
                .endControlFlow()
                .endControlFlow()
                .addStatement("throw new $T($S + colName)", IllegalArgumentException.class, "Can't find column ")
                .build();
    }

    private CodeBlock buildKeysWhereCodeBlock() {
        String where = " WHERE ";
        String var = " = ? ";
        String and = " AND ";
        return CodeBlock.builder()
                .beginControlFlow("if (KEYS_WHERE == null)")
                .addStatement("$L keys = $T.of(values()).filter(col -> col.key).map(col -> col.colName).collect($T.toList())",
                        ParameterizedTypeName.get(List.class, String.class), Stream.class, Collectors.class)
                .addStatement("boolean first = true")
                .addStatement("$T builder = new $T()", StringBuilder.class, StringBuilder.class)
                .beginControlFlow("for (String key : keys)")
                .beginControlFlow("if (first)")
                .addStatement("builder.append($S).append(key).append($S)", where, var)
                .addStatement("first = false")
                .nextControlFlow("else")
                .addStatement("builder.append($S).append(key).append($S)", and, var)
                .endControlFlow()
                .endControlFlow()
                .endControlFlow()
                .addStatement("return KEYS_WHERE")
                .build();
    }

    private CodeBlock buildSelectablesCodeBlock() {
        return CodeBlock.builder()
                .beginControlFlow("if (SELECTABLES == null)")
                .addStatement("SELECTABLES = $S + $T.of(values()).map(col -> col.colName).collect($T.joining($S)) + $S",
                        "(", Stream.class, Collectors.class, ",", ")")
                .endControlFlow()
                .addStatement("return SELECTABLES")
                .build();
    }

    private CodeBlock buildEntityMapperCodeBlock() {
        return CodeBlock.builder()
                .beginControlFlow("if (ENTITY_MAPPER == null)")
                .addStatement("$T.Builder builder = new $T.Builder()", EntityMapper.class, EntityMapper.class)
                .beginControlFlow("for (Column col : Column.values())")
                .addStatement("builder.add(col.colName, col.type, col.setter, col.getter, col.key)")
                .endControlFlow()
                .addStatement("ENTITY_MAPPER = builder.build()")
                .endControlFlow()
                .addStatement("return ENTITY_MAPPER")
                .build();
    }

    private TypeName getEntityMapperType(TypeElement entity) {
        return ParameterizedTypeName.get(ClassName.get(EntityMapper.class), ClassName.get(entity));
    }

    private TypeSpec enumImpl(Accessor accessor, TypeElement entity) {
        String converter = accessor.getConverter();
        if (converter != null) {
            return TypeSpec.anonymousClassBuilder("$T.class, $S, entity -> $L.toSql(entity.$L()), (entity, val) -> entity.$L($L.fromSql(($T) val)), $L",
                    accessor.getBeforeConvertKlass(),
                    accessor.getName(),
                    converter,
                    accessor.getGetterSetter().getKey().getSimpleName(),
                    accessor.getGetterSetter().getValue().getSimpleName(),
                    converter,
                    accessor.getBeforeConvertKlass(),
                    accessor.isKey()
            ).build();
        } else {
            return TypeSpec.anonymousClassBuilder("$T.class, $S, $T::$L, (entity, val) -> entity.$L(($T) val), $L",
                    accessor.getGetterSetter().getKey().getReturnType(),
                    accessor.getName(),
                    ClassName.get(entity),
                    accessor.getGetterSetter().getKey().getSimpleName(),
                    accessor.getGetterSetter().getValue().getSimpleName(),
                    accessor.getGetterSetter().getKey().getReturnType(),
                    accessor.isKey()
            ).build();
        }
    }

    private MethodSpec addColumnConstructor(TypeElement entity) {
        return MethodSpec.constructorBuilder()
                .addParameter(getGenericClassType(), "type")
                .addParameter(String.class, COL_NAME)
                .addParameter(getColumnGetterType(entity), "getter")
                .addParameter(getColumnSetterType(entity), "setter")
                .addParameter(boolean.class, "key")
                .addStatement("this.getter = getter")
                .addStatement("this.setter = setter")
                .addStatement("this.colName = colName")
                .addStatement("this.type = type")
                .addStatement("this.key = key")
                .build();
    }

    private ParameterizedTypeName getGenericClassType() {
        return ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class));
    }

    private ParameterizedTypeName getColumnSetterType(TypeElement entity) {
        return ParameterizedTypeName.get(ClassName.get(ColumnSetter.class), TypeName.get(entity.asType()), WildcardTypeName.subtypeOf(Object.class));
    }

    private ParameterizedTypeName getColumnGetterType(TypeElement entity) {
        return ParameterizedTypeName.get(ClassName.get(ColumnGetter.class), TypeName.get(entity.asType()), WildcardTypeName.subtypeOf(Object.class));
    }

    private List<Accessor> asAccessors(ProcessingEnvironment processingEnv, List<Element> columns) {
        List<Accessor> values = new ArrayList<>();
        for (Element column : columns) {
            boolean key = column.getAnnotation(Id.class) != null;
            Converter converter = column.getAnnotation(Converter.class);
            ExecutableElement getter = findGetter(column);
            ExecutableElement setter = findSetter(column);
            values.add(new Accessor(processingEnv, column.getAnnotation(Column.class).name(), new AbstractMap.SimpleEntry<>(getter, setter), key, converter));
        }

        return values;
    }

    private Iterable<MethodSpec> buildDelegation(TypeElement entity) {
        List<? extends Element> elements = entity.getEnclosedElements();
        List<? extends ExecutableElement> methods = elements.stream()
                .filter(e -> e instanceof ExecutableElement)
                .map(ExecutableElement.class::cast)
                .filter(e -> !e.getKind().equals(ElementKind.CONSTRUCTOR))
                .collect(Collectors.toList());
        List<Map.Entry<Element, ExecutableElement>> joins = new ArrayList<>();
        for (Element element : elements) {
            if (hasJoinAnnotation(element)) {
                ExecutableElement getter = findGetter(element);
                joins.add(new AbstractMap.SimpleImmutableEntry<>(element, getter));
            }
        }

        List<MethodSpec> specs = new ArrayList<>();
        for (ExecutableElement m : methods) {
            Optional<Map.Entry<Element, ExecutableElement>> join = joins.stream()
                    .filter(p -> p.getValue().equals(m))
                    .findFirst();
            if (join.isPresent()) {
                specs.add(buildJoinMethod(entity, join.get()));
            } else {
                specs.add(buildDelegateMethod(m));
            }
        }
        return specs;
    }

    private MethodSpec buildDelegateMethod(ExecutableElement m) {
        MethodSpec.Builder builder = MethodSpec.overriding(m)
                .addStatement("$T.requireNonNull(this.entity)", Objects.class);
        String variables;
        variables = extractParameterNames(m);

        if (m.getReturnType() instanceof NoType) {
            builder.addStatement("this.entity.$L($L)", m.getSimpleName(), variables);
        } else {
            builder.addStatement("return this.entity.$L($L)", m.getSimpleName(), variables);
        }
        return builder.build();
    }

    private String extractParameterNames(ExecutableElement m) {
        if (!m.getParameters().isEmpty()) {
            return m.getParameters().stream()
                    .map(VariableElement::getSimpleName)
                    .collect(Collectors.joining(","));
        }

        return "";
    }

    private MethodSpec buildJoinMethod(TypeElement entity, Map.Entry<Element, ExecutableElement> join) {
        ExecutableElement method = join.getValue();
        ReturnTypeDefinition definition = new ReturnTypeDefinition(processingEnv, method.getReturnType());
        String runnerMethod;
        if (definition.isCollection()) {
            runnerMethod = "readAll";
        } else if (definition.isOptional()) {
            runnerMethod = "readOpt";
        } else {
            runnerMethod = "read";
        }

        Relationship relationship = join.getKey().getAnnotation(Relationship.class);
        Relationship.RelationshipColumn[] columns = relationship.columns();

        checkColumns(entity, definition.getRealClass(), columns);

        MethodSpec.Builder builder = MethodSpec.overriding(method)
                .addStatement("$T params = new $T<>()", ParameterizedTypeName.get(List.class, SqlParameter.class), ArrayList.class);
        for (Relationship.RelationshipColumn column : columns) {
            Map.Entry<? extends Element, String> targetColumn = findColumn(definition.getRealClass(), column.targetColumn())
                    .orElseThrow(() -> new ProcessorException("Can't find target column"));
            buildJoinParam(column, builder, targetColumn, entity);
        }
        String wheres = createJoinWhere(columns);
        return builder.addStatement("return $T.getInstance($T.class).$L($T.class, $T.getCurrent().getSimpleSql($T.class) +  $S, params)",
                QueryRunner.class, definition.getRealClass(), runnerMethod, definition.getRealClass(), DelegatesService.class,
                definition.getRealClass(), wheres)
                .build();
    }

    private String createJoinWhere(Relationship.RelationshipColumn[] columns) {
        boolean first = true;
        StringBuilder builder = new StringBuilder();
        for (Relationship.RelationshipColumn column : columns) {
            String target = column.targetColumn();
            if (first) {
                builder.append("WHERE ").append(target).append(" = ? ");
                first = false;
            } else {
                builder.append("AND ").append(target).append(" = ?");
            }
        }

        return " " + builder.toString();
    }

    private void buildJoinParam(Relationship.RelationshipColumn column, MethodSpec.Builder builder,
                                Map.Entry<? extends Element, String> targetColumn,
                                TypeElement entity) {
        VariableElement element = (VariableElement) targetColumn.getKey();
        if (!column.sourceColumn().isEmpty()) {
            Map.Entry<? extends Element, String> sourceColumn = findColumn(entity, column.sourceColumn())
                    .orElseThrow(() -> new ProcessorException("Can't find source column"));
            builder.addStatement("params.add(new $T($LDelegate.Column.findColumn($S).getGetter().apply(this.entity), $T.find($T.class).getSetter()))",
                    SqlParameter.class, entity.getSimpleName(), sourceColumn.getValue(), SqlAccessor.class, element.asType());
        } else {
            String defaultValue = column.defaultValue();
            builder.addStatement("params.add(new $T($T.$L.toValue($S), $T.find($T.class).getSetter()))",
                    SqlParameter.class, ParameterConverter.class, column.converter(), defaultValue, SqlAccessor.class, element.asType());
        }
    }

    private void checkColumns(TypeElement entity, TypeElement realClass, Relationship.RelationshipColumn[] columns) {
        for (Relationship.RelationshipColumn column : columns) {
            if (column.sourceColumn().isEmpty() && column.defaultValue().isEmpty()) {
                throw new ProcessorException("Source column or default value is mandatory");
            }

            if (!column.sourceColumn().isEmpty()) {
                Optional<String> sourceColumn = findColumn(entity, column.sourceColumn())
                        .map(Map.Entry::getValue);

                if (!sourceColumn.isPresent()) {
                    throw new ProcessorException(String.format("Can't find source column %s for entity %s", column.sourceColumn(), entity));
                }
            }

            Optional<String> targetColumn = findColumn(realClass, column.targetColumn())
                    .map(Map.Entry::getValue);
            if (!targetColumn.isPresent()) {
                throw new ProcessorException(String.format("Can't find target column %s for entity %s", column.targetColumn(), realClass));
            }
        }
    }

    private Optional<? extends Map.Entry<? extends Element, String>> findColumn(TypeElement entity, String colName) {
        return entity.getEnclosedElements()
                .stream()
                .filter(el -> el.getAnnotation(Column.class) != null)
                .filter(el -> el.getAnnotation(Column.class).name().equalsIgnoreCase(colName))
                .map(el -> new AbstractMap.SimpleEntry<>(el, el.getAnnotation(Column.class).name()))
                .findFirst();
    }

    private ExecutableElement findGetter(Element element) {
        String fieldName = element.getSimpleName().toString();
        fieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String getter = "get" + fieldName;
        try {
            return findExecutable(element, getter, "Can't find getter for field " + element.getSimpleName());
        } catch (ProcessorException ex) {
            getter = "is" + fieldName;
            return findExecutable(element, getter, "Can't find getter for field " + element.getSimpleName());
        }
    }

    private ExecutableElement findSetter(Element element) {
        String fieldName = element.getSimpleName().toString();
        fieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String setter = "set" + fieldName;
        return findExecutable(element, setter, "Can't find setter for field " + element.getSimpleName());
    }

    private ExecutableElement findExecutable(Element element, String name, String errorMessage) {
        return element.getEnclosingElement()
                .getEnclosedElements()
                .stream()
                .filter(ele -> ele.getSimpleName().contentEquals(name))
                .findFirst()
                .map(ExecutableElement.class::cast)
                .orElseThrow(() -> new ProcessorException(errorMessage));
    }

    private boolean hasJoinAnnotation(Element ele) {
        return ele.getAnnotation(Relationship.class) != null;
    }
}
