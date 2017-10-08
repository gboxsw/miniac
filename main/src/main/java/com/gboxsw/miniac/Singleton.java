package com.gboxsw.miniac;

import java.lang.annotation.*;

/**
 * Identifies a device, a module, or a gateway that can have at most one
 * instance.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Singleton {

}
