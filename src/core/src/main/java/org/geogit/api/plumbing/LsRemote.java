/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import java.io.IOException;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Ref;
import org.geogit.api.Remote;
import org.geogit.remote.IRemoteRepo;
import org.geogit.remote.RemoteUtils;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;

/**
 * Connects to the specified remote, retrieves its {@link Ref refs}, closes the remote connection
 * and returns the list of remote references.
 */
public class LsRemote extends AbstractGeoGitOp<ImmutableSet<Ref>> {

    private Supplier<Optional<Remote>> remote;

    private boolean getHeads;

    private boolean getTags;

    private boolean local;

    public LsRemote() {
        Optional<Remote> abstent = Optional.absent();
        this.remote = Suppliers.ofInstance(abstent);
        this.getHeads = true;
        this.getTags = true;
    }

    public LsRemote setRemote(Supplier<Optional<Remote>> remote) {
        this.remote = remote;
        return this;
    }

    /**
     * @param getHeads tells whether to retrieve remote heads, defaults to {@code true}
     */
    public LsRemote retrieveHeads(boolean getHeads) {
        this.getHeads = getHeads;
        return this;
    }

    /**
     * @param getTags tells whether to retrieve remote tags, defaults to {@code true}
     */
    public LsRemote retrieveTgs(boolean getTags) {
        this.getTags = getTags;
        return this;
    }

    /**
     * @param local if {@code true} retrieves the refs of the remote repository known to the local
     *        repository instead (i.e. those under the {@code refs/remotes/<remote name>} namespace
     *        in the local repo. Defaults to {@code false}
     */
    public LsRemote retrieveLocalRefs(boolean local) {
        this.local = local;
        return this;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ImmutableSet<Ref> call() {
        Preconditions.checkState(remote.get().isPresent(), "Remote was not provided");
        final Remote remoteConfig = remote.get().get();

        if (local) {
            return locallyKnownRefs(remoteConfig);
        }
        getProgressListener().setDescription("Obtaining remote " + remoteConfig.getName());
        IRemoteRepo remoteRepo = RemoteUtils.newRemote(remoteConfig);
        getProgressListener().setDescription("Connecting to remote " + remoteConfig.getName());
        try {
            remoteRepo.open();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        getProgressListener().setDescription(
                "Connected to remote " + remoteConfig.getName() + ". Retrieving references");
        ImmutableSet<Ref> remoteRefs;
        try {
            remoteRefs = remoteRepo.listRefs(getHeads, getTags);
        } finally {
            try {
                remoteRepo.close();
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
        return remoteRefs;
    }

    /**
     * @see ForEachRef
     */
    private ImmutableSet<Ref> locallyKnownRefs(final Remote remoteConfig) {
        Predicate<Ref> filter = new Predicate<Ref>() {
            final String prefix = Ref.REMOTES_PREFIX + remoteConfig.getName() + "/";

            @Override
            public boolean apply(Ref input) {
                return input.getName().startsWith(prefix);
            }
        };
        return command(ForEachRef.class).setFilter(filter).call();
    }

}