package com.github.xiaolyuh.valve.merge;

import com.github.xiaolyuh.TagOptions;
import com.github.xiaolyuh.utils.NotifyUtil;
import com.intellij.openapi.project.Project;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitRepository;

/**
 * 分支合并阀门
 *
 * @author yuhao.wang3
 * @since 2020/4/7 16:42
 */
public class MergeValve extends Valve {
    private static MergeValve valve = new MergeValve();

    public static Valve getInstance() {
        return valve;
    }

    @Override
    public boolean invoke(Project project, GitRepository repository, String currentBranch, String targetBranch, TagOptions tagOptions) {
        GitCommandResult result = gitFlowPlus.mergeBranchAndPush(repository, currentBranch, targetBranch, tagOptions);
        if (result.success()) {
            NotifyUtil.notifySuccess(project, "Success", String.format("%s 分支已经合并到了 %s 分支，并推送到了远程仓库", currentBranch, targetBranch));
            return true;
        }

        NotifyUtil.notifyError(project, "Error", result.getErrorOutputAsJoinedString());
        return false;
    }
}
