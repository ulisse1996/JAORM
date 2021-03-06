package io.github.ulisse1996.jaorm.annotation;

import java.lang.annotation.*;

/**
 * Define a Primary Key Column that can be used by Jaorm for select/update/delete an Entity.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
@Documented
public @interface Id {

    /**
     * Define if current Id is also an auto-generated key.
     * Jaorm retrieve and set after persist event generated keys in the current Entity
     */
    boolean autoGenerated() default false;
}
