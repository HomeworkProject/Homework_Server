package de.mlessmann.updates_old;

import java.util.Optional;

/**
 * Created by Life4YourGames on 06.07.16.
 */
public interface IUpdateIndex {

    public Optional<IReleaseInfo> getRelease(String releaseName);



}
