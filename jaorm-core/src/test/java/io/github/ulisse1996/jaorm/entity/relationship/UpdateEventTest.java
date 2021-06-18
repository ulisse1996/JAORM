package io.github.ulisse1996.jaorm.entity.relationship;

import io.github.ulisse1996.jaorm.Arguments;
import io.github.ulisse1996.jaorm.BaseDao;
import io.github.ulisse1996.jaorm.entity.event.PostUpdate;
import io.github.ulisse1996.jaorm.entity.event.PreUpdate;
import io.github.ulisse1996.jaorm.exception.PersistEventException;
import io.github.ulisse1996.jaorm.spi.DelegatesService;
import io.github.ulisse1996.jaorm.spi.QueriesService;
import io.github.ulisse1996.jaorm.spi.QueryRunner;
import io.github.ulisse1996.jaorm.spi.RelationshipService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class UpdateEventTest extends EventTest {

    private final UpdateEvent testSubject = new UpdateEvent();

    @Test
    void should_throw_exception_for_unimplemented_method() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> testSubject.applyAndReturn(new Object())); // NOSONAR
    }

    @ParameterizedTest
    @MethodSource("getRelationship")
    @SuppressWarnings("unchecked")
    void should_insert_values_for_linked_entities(Relationship<Entity> tree) {
        RelationshipService relationshipService = Mockito.mock(RelationshipService.class);
        QueryRunner queryRunner = Mockito.mock(QueryRunner.class);
        DelegatesService delegatesService = Mockito.mock(DelegatesService.class);
        QueriesService queriesService = Mockito.mock(QueriesService.class);
        BaseDao<RelEntity> baseDao = Mockito.mock(BaseDao.class);
        try (MockedStatic<RelationshipService> mkRel = Mockito.mockStatic(RelationshipService.class);
             MockedStatic<QueryRunner> mkRunner = Mockito.mockStatic(QueryRunner.class);
             MockedStatic<DelegatesService> mkDelegates = Mockito.mockStatic(DelegatesService.class);
             MockedStatic<QueriesService> mkQueries = Mockito.mockStatic(QueriesService.class)) {
            mkRel.when(RelationshipService::getInstance)
                    .thenReturn(relationshipService);
            mkRunner.when(() -> QueryRunner.getInstance(Entity.class))
                    .thenReturn(queryRunner);
            mkDelegates.when(DelegatesService::getInstance)
                    .thenReturn(delegatesService);
            mkQueries.when(QueriesService::getInstance)
                    .thenReturn(queriesService);

            Mockito.when(relationshipService.getRelationships(Entity.class))
                    .thenReturn(tree);
            Mockito.when(delegatesService.getDeleteSql(Mockito.any()))
                    .thenReturn("");
            Mockito.when(delegatesService.asArguments(Mockito.any()))
                    .thenReturn(Arguments.empty());
            Mockito.when(delegatesService.asWhere(Mockito.any()))
                    .thenReturn(Arguments.empty());
            Mockito.when(queriesService.getBaseDao(RelEntity.class))
                    .thenReturn(baseDao);

            testSubject.apply(new Entity());

            Mockito.verify(queryRunner)
                    .update(Mockito.any(), Mockito.any());
            Mockito.verify(baseDao)
                    .update(Mockito.any(RelEntity.class));
        }
    }

    @Test
    void should_apply_pre_update() throws Exception {
        UpdateEvent subject = Mockito.spy(testSubject);
        PreUpdate<?> mock = Mockito.mock(PreUpdate.class);
        Relationship<?> fakeRel = new Relationship<>(Object.class);
        RelationshipService relationshipService = Mockito.mock(RelationshipService.class);
        QueryRunner runner = Mockito.mock(QueryRunner.class);
        DelegatesService delegatesService = Mockito.mock(DelegatesService.class);
        try (MockedStatic<QueryRunner> runnerMk = Mockito.mockStatic(QueryRunner.class);
             MockedStatic<RelationshipService> relMk = Mockito.mockStatic(RelationshipService.class);
             MockedStatic<DelegatesService> delegateMk = Mockito.mockStatic(DelegatesService.class)) {
            runnerMk.when(() -> QueryRunner.getInstance(Mockito.any()))
                    .thenReturn(runner);
            relMk.when(RelationshipService::getInstance)
                    .thenReturn(relationshipService);
            delegateMk.when(DelegatesService::getInstance)
                    .thenReturn(delegatesService);
            Mockito.when(delegatesService.asWhere(Mockito.any()))
                    .thenReturn(Arguments.empty());
            Mockito.when(delegatesService.asArguments(Mockito.any()))
                    .thenReturn(Arguments.empty());
            Mockito.when(relationshipService.getRelationships(Mockito.any()))
                    .then(onMock -> fakeRel);
            Mockito.doNothing()
                    .when(subject).doPreApply(Mockito.any(), Mockito.any());
            subject.apply(mock);
            Mockito.verify(mock).preUpdate();
        }
    }

    @Test
    void should_apply_post_update() throws Exception {
        UpdateEvent subject = Mockito.spy(testSubject);
        PostUpdate<?> mock = Mockito.mock(PostUpdate.class);
        Relationship<?> fakeRel = new Relationship<>(Object.class);
        RelationshipService relationshipService = Mockito.mock(RelationshipService.class);
        QueryRunner runner = Mockito.mock(QueryRunner.class);
        DelegatesService delegatesService = Mockito.mock(DelegatesService.class);
        try (MockedStatic<QueryRunner> runnerMk = Mockito.mockStatic(QueryRunner.class);
             MockedStatic<RelationshipService> relMk = Mockito.mockStatic(RelationshipService.class);
             MockedStatic<DelegatesService> delegateMk = Mockito.mockStatic(DelegatesService.class)) {
            runnerMk.when(() -> QueryRunner.getInstance(Mockito.any()))
                    .thenReturn(runner);
            relMk.when(RelationshipService::getInstance)
                    .thenReturn(relationshipService);
            delegateMk.when(DelegatesService::getInstance)
                    .thenReturn(delegatesService);
            Mockito.when(delegatesService.asWhere(Mockito.any()))
                    .thenReturn(Arguments.empty());
            Mockito.when(delegatesService.asArguments(Mockito.any()))
                    .thenReturn(Arguments.empty());
            Mockito.when(relationshipService.getRelationships(Mockito.any()))
                    .then(onMock -> fakeRel);
            Mockito.doNothing()
                    .when(subject).doPreApply(Mockito.any(), Mockito.any());
            subject.apply(mock);
            Mockito.verify(mock).postUpdate();
        }
    }

    @Test
    void should_throw_exception_for_pre_update() throws Exception {
        UpdateEvent subject = Mockito.spy(testSubject);
        PreUpdate<?> mock = Mockito.mock(PreUpdate.class);
        Relationship<?> fakeRel = new Relationship<>(Object.class);
        RelationshipService relationshipService = Mockito.mock(RelationshipService.class);
        QueryRunner runner = Mockito.mock(QueryRunner.class);
        DelegatesService delegatesService = Mockito.mock(DelegatesService.class);
        try (MockedStatic<QueryRunner> runnerMk = Mockito.mockStatic(QueryRunner.class);
             MockedStatic<RelationshipService> relMk = Mockito.mockStatic(RelationshipService.class);
             MockedStatic<DelegatesService> delegateMk = Mockito.mockStatic(DelegatesService.class)) {
            runnerMk.when(() -> QueryRunner.getInstance(Mockito.any()))
                    .thenReturn(runner);
            relMk.when(RelationshipService::getInstance)
                    .thenReturn(relationshipService);
            delegateMk.when(DelegatesService::getInstance)
                    .thenReturn(delegatesService);
            Mockito.when(delegatesService.asWhere(Mockito.any()))
                    .thenReturn(Arguments.empty());
            Mockito.when(delegatesService.asArguments(Mockito.any()))
                    .thenReturn(Arguments.empty());
            Mockito.when(relationshipService.getRelationships(Mockito.any()))
                    .then(onMock -> fakeRel);
            Mockito.doThrow(Exception.class)
                    .when(mock).preUpdate();
            Mockito.doNothing()
                    .when(subject).doPreApply(Mockito.any(), Mockito.any());
            Assertions.assertThrows(PersistEventException.class, () -> subject.apply(mock));
        }
    }

    @Test
    void should_throw_exception_for_post_update() throws Exception {
        UpdateEvent subject = Mockito.spy(testSubject);
        PostUpdate<?> mock = Mockito.mock(PostUpdate.class);
        Relationship<?> fakeRel = new Relationship<>(Object.class);
        RelationshipService relationshipService = Mockito.mock(RelationshipService.class);
        QueryRunner runner = Mockito.mock(QueryRunner.class);
        DelegatesService delegatesService = Mockito.mock(DelegatesService.class);
        try (MockedStatic<QueryRunner> runnerMk = Mockito.mockStatic(QueryRunner.class);
             MockedStatic<RelationshipService> relMk = Mockito.mockStatic(RelationshipService.class);
             MockedStatic<DelegatesService> delegateMk = Mockito.mockStatic(DelegatesService.class)) {
            runnerMk.when(() -> QueryRunner.getInstance(Mockito.any()))
                    .thenReturn(runner);
            relMk.when(RelationshipService::getInstance)
                    .thenReturn(relationshipService);
            delegateMk.when(DelegatesService::getInstance)
                    .thenReturn(delegatesService);
            Mockito.when(delegatesService.asWhere(Mockito.any()))
                    .thenReturn(Arguments.empty());
            Mockito.when(delegatesService.asArguments(Mockito.any()))
                    .thenReturn(Arguments.empty());
            Mockito.when(relationshipService.getRelationships(Mockito.any()))
                    .then(onMock -> fakeRel);
            Mockito.doThrow(Exception.class)
                    .when(mock).postUpdate();
            Mockito.doNothing()
                    .when(subject).doPreApply(Mockito.any(), Mockito.any());
            Assertions.assertThrows(PersistEventException.class, () -> subject.apply(mock));
        }
    }
}
