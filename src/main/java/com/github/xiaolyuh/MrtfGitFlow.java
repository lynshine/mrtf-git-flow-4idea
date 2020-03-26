package com.github.xiaolyuh;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandlerListener;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yuhao.wang3
 * @since 2020/3/23 9:52
 */
public interface MrtfGitFlow {

    /**
     * 获取实例
     *
     * @return GbmGit
     */
    @NotNull
    static MrtfGitFlow getInstance() {
        return ServiceManager.getService(MrtfGitFlow.class);
    }

    /**
     * 初始化插件
     *
     * @param repository GitRepository
     * @param reInit     是否从初始化
     */
    void addConfigToGit(GitRepository repository, boolean reInit);

    /**
     * 以远程master为根新创建本地分支
     *
     * @param repository    repository
     * @param master        主干分支
     * @param newBranchName 新建分支
     * @param listeners     结果监听
     * @return GitCommandResult
     */
    GitCommandResult newNewBranchBaseRemoteMaster(@NotNull GitRepository repository,
                                                  @Nullable String master,
                                                  @NotNull String newBranchName,
                                                  @Nullable GitLineHandlerListener... listeners);


    /**
     * 重建分支
     *
     * @param repository    repository
     * @param oldBranch     原来分支名称
     * @param newBranchName 新分支名称
     * @param listener      结果监听
     * @return GitCommandResult
     */
    GitCommandResult renameBranch(@NotNull GitRepository repository,
                                  @Nullable String oldBranch,
                                  @NotNull String newBranchName,
                                  @Nullable GitLineHandlerListener listener);


    /**
     * 删除分支
     *
     * @param repository repository
     * @param master     主干分支名称
     * @param branchName 需要删除的分支
     * @param listener   结果监听
     * @return GitCommandResult
     */
    GitCommandResult deleteBranch(@NotNull GitRepository repository,
                                  @Nullable String master,
                                  @Nullable String branchName,
                                  @Nullable GitLineHandlerListener listener);

    /**
     * 获取当前分支名称
     *
     * @param project project
     * @return GitCommandResult
     */
    String getCurrentBranch(@NotNull Project project);

    /**
     * 获取远程分支最后一次Commit信息
     *
     * @param repository GitRepository
     * @param branch     branch
     * @param listeners  listeners
     * @return GitCommandResult
     */
    GitCommandResult getRemoteLastCommit(@NotNull GitRepository repository,
                                         @Nullable String branch,
                                         @Nullable GitLineHandlerListener... listeners);

    /**
     * 合并分支
     *
     * @param repository    repository
     * @param currentBranch currentBranch
     * @param targetBranch  目标分支
     * @param tagName       tagName
     * @param errorListener listeners
     * @return GitCommandResult
     */
    GitCommandResult mergeBranchAndPush(@NotNull GitRepository repository,
                                        @Nullable String currentBranch,
                                        @Nullable String targetBranch,
                                        String tagName,
                                        @Nullable GitLineHandlerListener errorListener);

    /**
     * 加锁
     *
     * @param repository    GitCommandResult
     * @param currentBranch currentBranch
     * @param errorListener errorListener
     * @return 加锁成功=true
     */
    boolean lock(GitRepository repository, String currentBranch, GitLineHandlerListener... errorListener);

    /**
     * 解锁
     *
     * @param repository
     * @return
     */
    boolean unlock(GitRepository repository);
}