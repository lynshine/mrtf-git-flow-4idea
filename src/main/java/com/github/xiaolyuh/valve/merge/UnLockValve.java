package com.github.xiaolyuh.valve.merge;

import com.github.xiaolyuh.TagOptions;
import com.github.xiaolyuh.utils.NotifyUtil;
import com.intellij.openapi.project.Project;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitRepository;

/**
 * 解锁阀门
 *
 * @author yuhao.wang3
 * @since 2020/4/7 16:42
 */
public class UnLockValve extends Valve {
    private static UnLockValve valve = new UnLockValve();

    public static Valve getInstance() {
        return valve;
    }

    @Override
    public boolean invoke(Project project, GitRepository repository, String currentBranch, String targetBranch, TagOptions tagOptions) {
        GitCommandResult result = gitFlowPlus.unlock(repository);

        if (result.success()) {
            NotifyUtil.notifySuccess(repository.getProject(), "Success", "发布分支已解除锁定，可以再次点击[开始发布]");
            return true;
        }

        NotifyUtil.notifyError(repository.getProject(), "Error", String.format("发布分支解除锁定失败: %s", result.getErrorOutputAsJoinedString()));
        return false;
    }
}
