package com.github.xiaolyuh;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yuhao.wang3
 * @since 2020/3/23 9:52
 */
public interface GitFlowPlus {

    /**
     * 获取实例
     *
     * @return GbmGit
     */
    @NotNull
    static GitFlowPlus getInstance() {
        return ServiceManager.getService(GitFlowPlus.class);
    }

    /**
     * 初始化插件
     *
     * @param repository GitRepository
     */
    void addConfigToGit(GitRepository repository);

    /**
     * 以远程master为根新创建本地分支
     *
     * @param repository    repository
     * @param master        主干分支
     * @param newBranchName 新建分支
     * @return GitCommandResult
     */
    GitCommandResult newNewBranchBaseRemoteMaster(@NotNull GitRepository repository,
                                                  @Nullable String master,
                                                  @NotNull String newBranchName);


    /**
     * 删除分支
     *
     * @param repository repository
     * @param master     主干分支名称
     * @param branchName 需要删除的分支
     * @return GitCommandResult
     */
    GitCommandResult deleteBranch(@NotNull GitRepository repository,
                                  @Nullable String master,
                                  @Nullable String branchName);

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
     * @return String
     */
    String getRemoteLastCommit(@NotNull GitRepository repository,
                               @Nullable String branch);

    /**
     * 合并分支
     *
     * @param repository    repository
     * @param currentBranch currentBranch
     * @param targetBranch  目标分支
     * @param tagOptions    TagOptions
     * @return GitCommandResult
     */
    GitCommandResult mergeBranchAndPush(@NotNull GitRepository repository,
                                        @Nullable String currentBranch,
                                        @Nullable String targetBranch,
                                        TagOptions tagOptions);

    /**
     * 加锁
     *
     * @param repository    GitCommandResult
     * @param currentBranch currentBranch
     * @return 加锁成功=true
     */
    boolean lock(GitRepository repository, String currentBranch);

    /**
     * 解锁
     *
     * @param repository
     * @return
     */
    GitCommandResult unlock(GitRepository repository);

    /**
     * 判断发布分支是否锁定(缓存)
     *
     * @param project product
     * @return true 表示锁定
     */
    boolean isLock(Project project);

    /**
     * 判断发布分支是否锁定(远程同步)
     *
     * @param repository repository
     * @return true 表示锁定
     */
    boolean isLock(GitRepository repository);

    /**
     * 第三方通知
     *
     * @param repository repository
     */
    void thirdPartyNotify(GitRepository repository);

    /**
     * 是否存在未提交文件
     *
     * @param project
     * @return
     */
    boolean isExistChangeFile(@NotNull Project project);

    /**
     * 获取当前Git账号的邮箱
     *
     * @param repository
     * @return
     */
    String getUserEmail(GitRepository repository);

    /**
     * 判断tag是否存在
     *
     * @param repository
     * @param tagName
     * @return
     */
    boolean isExistTag(GitRepository repository, String tagName);
}