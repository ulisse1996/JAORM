package io.github.ulisse1996.jaorm.entity.relationship;

import io.github.ulisse1996.jaorm.entity.EntityDelegate;
import io.github.ulisse1996.jaorm.entity.EntityMapper;
import io.github.ulisse1996.jaorm.entity.Result;
import org.junit.jupiter.params.provider.Arguments;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class EventTest {

    protected static class Entity {

        public Result<RelEntity> getRelEntityOpt() {
            return Result.of(new RelEntity());
        }

        public List<RelEntity> getRelEntityColl() {
            return Collections.singletonList(new RelEntity());
        }

        public RelEntity getRelEntity() {
            return new RelEntity();
        }
    }

    protected static class MyEntityDelegate extends Entity implements EntityDelegate<Entity> {

        private Entity entity;

        @Override
        public Supplier<Entity> getEntityInstance() {
            return null;
        }

        @Override
        public EntityMapper<Entity> getEntityMapper() {
            return null;
        }

        @Override
        public void setEntity(ResultSet rs) throws SQLException {

        }

        @Override
        public void setFullEntity(Entity entity) {
            this.entity = entity;
        }

        @Override
        public Entity getEntity() {
            return entity;
        }

        @Override
        public String getBaseSql() {
            return null;
        }

        @Override
        public String getKeysWhere() {
            return null;
        }

        @Override
        public String getInsertSql() {
            return null;
        }

        @Override
        public String[] getSelectables() {
            return new String[0];
        }

        @Override
        public String getTable() {
            return null;
        }

        @Override
        public String getUpdateSql() {
            return null;
        }

        @Override
        public String getDeleteSql() {
            return null;
        }

        @Override
        public boolean isModified() {
            return false;
        }
    }

    protected static class RelEntity {}

    protected static Stream<Arguments> getRelationship() {
        Relationship<Entity> tree1 = new Relationship<>(Entity.class);
        tree1.add(new Relationship.Node<>(Entity::getRelEntity, false, false, EntityEventType.values()));
        Relationship<Entity> tree2 = new Relationship<>(Entity.class);
        tree2.add(new Relationship.Node<>(Entity::getRelEntityOpt, true, false, EntityEventType.values()));
        Relationship<Entity> tree3 = new Relationship<>(Entity.class);
        tree3.add(new Relationship.Node<>(Entity::getRelEntityColl, false, true, EntityEventType.values()));
        return Stream.of(
                Arguments.arguments(tree1),
                Arguments.arguments(tree2),
                Arguments.arguments(tree3)
        );
    }
}
