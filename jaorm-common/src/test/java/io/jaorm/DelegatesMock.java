package io.jaorm;

import io.jaorm.entity.*;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

public class DelegatesMock extends DelegatesService {

    @Override
    protected Map<Class<?>, Supplier<? extends EntityDelegate<?>>> getDelegates() {
        return Collections.singletonMap(MyEntity.class, MyEntityDelegate::new);
    }

    public static class MyEntity implements PrePersist<MyEntity, IllegalArgumentException>, PostPersist<MyEntity, IllegalArgumentException> {

        private String field1;
        private BigDecimal field2;

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        public BigDecimal getField2() {
            return field2;
        }

        public void setField2(BigDecimal field2) {
            this.field2 = field2;
        }

        @Override
        public void postPersist(MyEntity entity) throws IllegalArgumentException {

        }

        @Override
        public void prePersist(MyEntity entity) throws IllegalArgumentException {

        }
    }

    public static class MyEntityDelegate extends MyEntity implements EntityDelegate<MyEntity> {

        private MyEntity entity;

        @Override
        public String getField1() {
            return entity.getField1();
        }

        @Override
        public BigDecimal getField2() {
            return entity.getField2();
        }

        @Override
        public Supplier<MyEntity> getEntityInstance() {
            return MyEntity::new;
        }

        @Override
        public EntityMapper<MyEntity> getEntityMapper() {
            EntityMapper.Builder<MyEntity> builder = new EntityMapper.Builder<>();
            builder.add("FIELD1", String.class, (entity, value) -> entity.setField1((String) value), MyEntity::getField1, true);
            builder.add("FIELD2", BigDecimal.class, (entity, value) -> entity.setField2((BigDecimal) value), MyEntity::getField2, false);
            return builder.build();
        }

        @Override
        public void setEntity(ResultSet rs) throws SQLException {
            this.entity = toEntity(rs);
        }

        @Override
        public void setFullEntity(MyEntity entity) {
            this.entity = entity;
        }

        @Override
        public String getBaseSql() {
            return "SELECT FIELD1, FIELD2 FROM MYENTITY";
        }

        @Override
        public String getKeysWhere() {
            return " WHERE 1 = 1";
        }

        @Override
        public String getInsertSql() {
            return "INSERT INTO MYENTITY (FIELD1, FIELD2) VALUES (?,?)";
        }
    }
}