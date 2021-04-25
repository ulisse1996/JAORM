package io.jaorm.integration.test;

import io.jaorm.custom.SqlAccessorFeature;
import io.jaorm.entity.EntityComparator;
import io.jaorm.entity.EntityDelegate;
import io.jaorm.entity.sql.SqlAccessor;
import io.jaorm.integration.test.entity.*;
import io.jaorm.integration.test.query.*;
import io.jaorm.spi.QueriesService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

class QueryIT extends AbstractIT {

    @ParameterizedTest
    @MethodSource("getSqlTests")
    void should_create_new_entity(HSQLDBProvider.DatabaseType type, String initSql) {
        setDataSource(type, initSql);

        UserDAO dao = QueriesService.getInstance().getQuery(UserDAO.class);

        User user = new User();
        user.setId(1);

        User inserted = dao.insert(user);
        Assertions.assertTrue(inserted instanceof EntityDelegate);

        User found = dao.read(user);
        Assertions.assertNotSame(user, found);
        Assertions.assertEquals(user.getId(), found.getId());
    }

    @ParameterizedTest
    @MethodSource("getSqlTests")
    void should_update_all_entity(HSQLDBProvider.DatabaseType type, String initSql) {
        setDataSource(type, initSql);

        User user = getUser(1);
        User user2 = getUser(2);
        User user3 = getUser(3);
        User user4 = getUser(4);
        User user5 = getUser(5);

        UserDAO dao = QueriesService.getInstance().getQuery(UserDAO.class);

        dao.insert(Arrays.asList(user, user2, user3, user4, user5));

        List<User> users = dao.readAll();

        Assertions.assertEquals(5, users.size());
        users.sort(Comparator.comparing(User::getId));

        Assertions.assertEquals(users.get(0), user);
        Assertions.assertEquals(users.get(1), user2);
        Assertions.assertEquals(users.get(2), user3);
        Assertions.assertEquals(users.get(3), user4);
        Assertions.assertEquals(users.get(4), user5);

        List<User> modUsers = users.stream()
                .peek(t -> t.setDepartmentId(t.getId() + 10))
                .collect(Collectors.toList());

        dao.update(modUsers);

        users = dao.readAll();
        Assertions.assertEquals(modUsers, users);
    }

    @ParameterizedTest
    @MethodSource("getSqlTests")
    void should_delete_entity(HSQLDBProvider.DatabaseType type, String initSql) {
        setDataSource(type, initSql);

        User user = new User();
        user.setId(1);
        user.setName("NAME");
        user.setDepartmentId(1);

        UserDAO dao = QueriesService.getInstance().getQuery(UserDAO.class);

        user = dao.insert(user);
        Optional<User> optionalUser = dao.readOpt(user);

        Assertions.assertTrue(optionalUser.isPresent());
        Assertions.assertTrue(EntityComparator.getInstance(User.class).equals(user, optionalUser.get()));

        dao.delete(user);

        optionalUser = dao.readOpt(user);
        Assertions.assertFalse(optionalUser.isPresent());
    }

    @ParameterizedTest
    @MethodSource("getSqlTests")
    void should_do_cascade_operation(HSQLDBProvider.DatabaseType type, String initSql) {
        setDataSource(type, initSql);

        CityDAO cityDAO = QueriesService.getInstance().getQuery(CityDAO.class);

        City city = new City();
        city.setCityId(10);
        city.setName("CITY");

        Store store = new Store();
        store.setStoreId(1);
        store.setName("NAME1");
        store.setCityId(city.getCityId());

        city.setStores(Collections.singletonList(store));

        cityDAO.insert(city);

        List<Store> stores = QueriesService.getInstance().getQuery(StoreDAO.class).readAll();
        Assertions.assertEquals(1, stores.size());
        Assertions.assertTrue(EntityComparator.getInstance(Store.class).equals(store, stores.get(0)));

        // Now read and update

        city = cityDAO.read(city);
        city.getStores()
                .forEach(s -> s.setName(s.getName() + "_AFTER"));
        cityDAO.update(city);

        stores = QueriesService.getInstance().getQuery(StoreDAO.class).readAll();
        Assertions.assertEquals(1, stores.size());
        Assertions.assertTrue(stores.get(0).getName().endsWith("_AFTER"));
    }

    @ParameterizedTest
    @MethodSource("getSqlTests")
    void should_check_unique_with_distinct(HSQLDBProvider.DatabaseType type, String initSql) {
        setDataSource(type, initSql);

        City city = new City();
        city.setCityId(1);
        city.setName("CITY");

        City city2 = new City();
        city2.setCityId(2);
        city2.setName("CITY");

        City city3 = new City();
        city3.setCityId(3);
        city3.setName("CITY");

        CityDAO cityDAO = QueriesService.getInstance().getQuery(CityDAO.class);

        cityDAO.insert(Arrays.asList(city, city2, city3));

        List<City> cities = cityDAO.readAll();
        Assertions.assertEquals(3, cities.size());

        cities = cities.stream()
                .sorted(Comparator.comparing(City::getCityId))
                .filter(EntityComparator.distinct(City::getName))
                .collect(Collectors.toList());

        Assertions.assertEquals(1, cities.size());
        Assertions.assertTrue(EntityComparator.getInstance(City.class).equals(city, cities.get(0)));
    }

    @ParameterizedTest
    @MethodSource("getSqlTests")
    void should_insert_entity_with_auto_generated_key(HSQLDBProvider.DatabaseType type, String initSql) {
        setDataSource(type, initSql);

        AutoGenerated generated = new AutoGenerated();
        generated.setName("NAME");
        AutoGenDao query = QueriesService.getInstance().getQuery(AutoGenDao.class);
        generated = query.insert(generated);
        Assertions.assertEquals("NAME", generated.getName());
        Assertions.assertEquals(BigDecimal.ONE, generated.getColGen());
    }

    @ParameterizedTest
    @MethodSource("getSqlTests")
    void should_use_custom_sql_accessor_feature(HSQLDBProvider.DatabaseType type, String initSql) {
        setDataSource(type, initSql);

        CustomAccessorDAO dao = QueriesService.getInstance().getQuery(CustomAccessorDAO.class);
        try {
            dao.select(CustomAccessor.MyEnumCustom.VAL);
        } catch (Exception ex) {
            Assertions.fail("Should not throw exception", ex);
        }
    }

    public static class MyFeature implements SqlAccessorFeature {

        @Override
        public <R> SqlAccessor findCustom(Class<R> klass) {
            if (CustomAccessor.MyEnumCustom.class.equals(klass)) {
                return new SqlAccessor(
                        CustomAccessor.MyEnumCustom.class,
                        (rs, colName) -> CustomAccessor.MyEnumCustom.valueOf(rs.getString(colName)),
                        (pr, index, value) -> pr.setString(index, ((CustomAccessor.MyEnumCustom) value).name())
                ) {};
            }

            return null;
        }
    }

    private User getUser(int i) {
        User user = new User();
        user.setId(i);
        user.setName("USER_" + i);
        return user;
    }
}
