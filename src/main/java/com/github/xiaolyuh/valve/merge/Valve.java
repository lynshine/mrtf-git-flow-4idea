package com.github.xiaolyuh.valve.merge;

import com.github.xiaolyuh.GitFlowPlus;
import com.github.xiaolyuh.TagOptions;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;

/**
 * @author yuhao.wang3
 * @since 2020/4/7 16:33
 */
public abstract class Valve {
    GitFlowPlus gitFlowPlus = GitFlowPlus.getInstance();

    /**
     * @param project       {@link Project}
     * @param repository    {@link GitRepository}
     * @param currentBranch 当前分支
     * @param targetBranch  合并的目标分支
     * @param tagOptions    tag参数
     * @return true-表示执行下一个阀门
     */
    public abstract boolean invoke(Project project, GitRepository repository, String currentBranch, String targetBranch, TagOptions tagOptions);
}
