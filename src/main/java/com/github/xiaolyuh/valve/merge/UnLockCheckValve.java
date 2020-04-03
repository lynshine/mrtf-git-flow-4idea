package com.github.xiaolyuh.valve.merge;

import com.github.xiaolyuh.TagOptions;
import com.github.xiaolyuh.utils.ConfigUtil;
import com.github.xiaolyuh.utils.NotifyUtil;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;

/**
 * 解锁校验
 *
 * @author yuhao.wang3
 * @since 2020/4/7 16:42
 */
public class UnLockCheckValve extends Valve {
    private static UnLockCheckValve valve = new UnLockCheckValve();

    public static Valve getInstance() {
        return valve;
    }

    @Override
    public boolean invoke(Project project, GitRepository repository, String currentBranch, String targetBranch, TagOptions tagOptions) {
        String release = ConfigUtil.getConfig(project).get().getReleaseBranch();
        String lastCommitMsg = gitFlowPlus.getRemoteLastCommit(repository, release);
        String email = gitFlowPlus.getUserEmail(repository);
        // 校验操作人
        if (!lastCommitMsg.contains(email)) {
            NotifyUtil.notifyError(project, "Error",
                    String.format("发布分支已被锁定，最后一次操作：%s ;\r\n如需强行发布，请先找对相应人点[发布失败]。", lastCommitMsg));
            return false;
        }

        // 校验锁定状态
        if (!gitFlowPlus.isLock(repository)) {
            NotifyUtil.notifyError(project, "Error", "呀！发布分支已经解锁了，当前操作已经被阻止！");
            return false;
        }
        return true;
    }
}
