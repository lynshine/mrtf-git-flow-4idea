/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.xiaolyuh.ui;

import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import git4idea.ui.branch.GitBranchWidget;
import org.jetbrains.annotations.NotNull;

/**
 * git flow 状态栏小部件
 *
 * @author yuhao.wang3
 */
public class GitFlowWidget extends GitBranchWidget implements GitRepositoryChangeListener {

    public GitFlowWidget(@NotNull Project project) {
        super(project);
    }

    @Override
    public void repositoryChanged(@NotNull GitRepository repository) {

    }
}
