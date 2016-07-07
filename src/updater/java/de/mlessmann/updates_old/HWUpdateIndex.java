package de.mlessmann.updates_old;

/**
 * Created by Life4YourGames on 06.07.16.
 */

/**
 * JSONType - "Object" or "Array", needed due to github-rel using array as top level JSON
 */
public @interface HWUpdateIndex {
    String JSONType() default "Object";
}
