package com.storyteller_f.common_ui_list_structure;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.storyteller_f.common_ui_list_structure.db.RepoDatabase;
import com.storyteller_f.common_ui_list_structure.model.Repo;
import com.storyteller_f.common_ui_list_structure.model.RepoRemoteKey;
import com.storyteller_f.ui_list.database.CommonRoomDatabase;

import java.util.List;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

/** @noinspection ALL*/
public class TestRepoComposite extends CommonRoomDatabase<Repo, RepoRemoteKey, RepoDatabase> {
    public TestRepoComposite(@NonNull RepoDatabase database) {
        super(database);
    }

    @Nullable
    @Override
    public Object clearOld(@NonNull Continuation<? super Unit> $completion) {
        getDatabase().reposDao().clearRepos($completion);
        getDatabase().remoteKeyDao().clearRemoteKeys($completion);
        return null;
    }

    @Nullable
    @Override
    public Object insertRemoteKey(@NonNull List<? extends RepoRemoteKey> remoteKeys, @NonNull Continuation<? super Unit> $completion) {
        getDatabase().remoteKeyDao().insertAll(remoteKeys, $completion);
        return null;
    }

    @Nullable
    @Override
    public Object getRemoteKey(@NonNull String id, @NonNull Continuation<? super RepoRemoteKey> $completion) {
        return getDatabase().remoteKeyDao().remoteKeysRepoId(id, $completion);
    }

    @Nullable
    @Override
    public Object insertAllData(@NonNull List<? extends Repo> repos, @NonNull Continuation<? super Unit> $completion) {
        getDatabase().reposDao().insertAll(repos, $completion);
        return null;
    }

    @Nullable
    @Override
    public Object deleteItemBy(@NonNull Repo repo, @NonNull Continuation<? super Unit> $completion) {
        getDatabase().reposDao().delete(repo, $completion);
        getDatabase().remoteKeyDao().delete(repo.remoteKeyId(), $completion);
        return null;
    }

    @Nullable
    @Override
    public Object deleteItemById(@NonNull String commonDatumId, @NonNull Continuation<? super Unit> $completion) {
        getDatabase().reposDao().delete(Long.parseLong(commonDatumId), $completion);
        getDatabase().remoteKeyDao().delete(commonDatumId, $completion);
        return null;
    }
}