package org.jqassistant.contrib.plugin.testimpactanalysis.impact;

import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitBranchDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitCommitDescriptor;
import de.kontext_e.jqassistant.plugin.git.store.descriptor.GitRepositoryDescriptor;
import org.jqassistant.contrib.plugin.testimpactanalysis.impact.set.*;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class TestsAffectedByLastGitCommitIT extends AbstractTestImpactAnalysisRuleIT {

    private static final String CONCEPT = "test-impact-analysis:TestsAffectedByLastGitCommit";

    @Test
    public void typeChanged() throws Exception {
        createGitHistory(Type.class);
        verify(Type.class, CONCEPT, Collections.emptyMap());
    }

    @Test
    public void subTypeChanged() throws Exception {
        createGitHistory(SubType.class);
        verify(SubType.class, CONCEPT, Collections.emptyMap());
    }

    @Test
    public void superTypeChanged() throws Exception {
        createGitHistory(SuperType.class);
        verify(SuperType.class, CONCEPT, Collections.emptyMap());
    }

    @Test
    public void transitiveTypeChanged() throws Exception {
        createGitHistory(TransitiveType.class);
        verify(TransitiveType.class, CONCEPT, Collections.emptyMap());
    }

    @Test
    public void transitiveSubTypeChanged() throws Exception {
        createGitHistory(TransitiveSubType.class);
        verify(TransitiveSubType.class, CONCEPT, Collections.emptyMap());
    }

    @Test
    public void transitiveSuperTypeChanged() throws Exception {
        createGitHistory(TransitiveSuperType.class);
        verify(TransitiveSuperType.class, CONCEPT, Collections.emptyMap());
    }

    private void createGitHistory(Class<?> changedType) {
        store.beginTransaction();
        GitCommitDescriptor head = createCommit(changedType);
        GitCommitDescriptor parent = createCommit(OtherType.class);
        head.getParents().add(parent);
        GitBranchDescriptor branch = store.create(GitBranchDescriptor.class);
        branch.setName("heads/master");
        branch.setHead(head);
        GitRepositoryDescriptor repository = store.create(GitRepositoryDescriptor.class);
        repository.setHead(head);
        repository.getBranches().add(branch);
        store.commitTransaction();
    }
}
