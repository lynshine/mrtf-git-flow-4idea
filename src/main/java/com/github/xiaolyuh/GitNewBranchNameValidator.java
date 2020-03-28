package com.github.xiaolyuh;

import com.intellij.openapi.ui.InputValidatorEx;
import git4idea.GitBranch;
import git4idea.branch.GitBranchUtil;
import git4idea.branch.GitBranchesCollection;
import git4idea.repo.GitRepository;
import git4idea.validators.GitRefNameValidator;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * 分支名称校验
 *
 * @author yuhao.wang3
 */
public final class GitNewBranchNameValidator implements InputValidatorEx {

    private final Collection<GitRepository> myRepositories;
    private String myErrorText;
    private String prefix;

    private GitNewBranchNameValidator(@NotNull Collection<GitRepository> repositories, String prefix) {
        this.myRepositories = repositories;
        this.prefix = prefix;
    }

    public static GitNewBranchNameValidator newInstance(@NotNull Collection<GitRepository> repositories, @NotNull String prefix) {
        return new GitNewBranchNameValidator(repositories, prefix);
    }

    @Override
    public boolean checkInput(@NotNull String inputString) {
        if (!GitRefNameValidator.getInstance().checkInput(inputString)) {
            myErrorText = "无效的分支名称";
            return false;
        }
        return checkBranchConflict(prefix + inputString);
    }

    private boolean checkBranchConflict(@NotNull String inputString) {
        if (isNotPermitted(inputString) || conflictsWithLocalBranch(inputString) || conflictsWithRemoteBranch(inputString)) {
            return false;
        }
        myErrorText = null;
        return true;
    }

    private boolean isNotPermitted(@NotNull String inputString) {
        if (inputString.equalsIgnoreCase("head")) {
            myErrorText = "分支名称 " + inputString + " 不可用";
            return true;
        }
        return false;
    }

    private boolean conflictsWithLocalBranch(@NotNull String inputString) {
        return conflictsWithLocalOrRemote(inputString, true, " 在本地仓库中已经存在");
    }

    private boolean conflictsWithRemoteBranch(@NotNull String inputString) {
        return conflictsWithLocalOrRemote(inputString, false, " 在远程仓库中已存在");
    }

    private boolean conflictsWithLocalOrRemote(@NotNull String inputString, boolean local, @NotNull String message) {
        int conflictsWithCurrentName = 0;
        for (GitRepository repository : myRepositories) {
            if (inputString.equals(repository.getCurrentBranchName())) {
                conflictsWithCurrentName++;
            } else {
                GitBranchesCollection branchesCollection = repository.getBranches();
                Collection<? extends GitBranch> branches = local ? branchesCollection.getLocalBranches() : branchesCollection.getRemoteBranches();
                for (GitBranch branch : branches) {
                    if (branch.getName().equals(inputString)) {
                        myErrorText = "分支名称 " + inputString + message;
                        if (myRepositories.size() > 1 && !allReposHaveBranch(inputString, local)) {
                            myErrorText += " in repository " + repository.getPresentableUrl();
                        }
                        return true;
                    }
                }
            }
        }
        if (conflictsWithCurrentName == myRepositories.size()) {
            myErrorText = "你当前正处于 " + inputString + " 分支";
            return true;
        }
        return false;
    }

    private boolean allReposHaveBranch(String inputString, boolean local) {
        for (GitRepository repository : myRepositories) {
            GitBranchesCollection branchesCollection = repository.getBranches();
            Collection<? extends GitBranch> branches = local ? branchesCollection.getLocalBranches() : branchesCollection.getRemoteBranches();
            if (!GitBranchUtil.convertBranchesToNames(branches).contains(inputString)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canClose(String inputString) {
        return checkInput(prefix + inputString);
    }

    @Override
    public String getErrorText(String inputString) {
        return myErrorText;
    }
}
