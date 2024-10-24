// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.spring.data.cosmos.repository.integration;

import com.azure.data.cosmos.PartitionKey;
import com.azure.spring.data.cosmos.core.CosmosTemplate;
import com.azure.spring.data.cosmos.repository.support.CosmosEntityInformation;
import com.google.common.collect.Lists;
import com.azure.spring.data.cosmos.domain.Project;
import com.azure.spring.data.cosmos.repository.TestRepositoryConfig;
import com.azure.spring.data.cosmos.repository.repository.ProjectRepository;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestRepositoryConfig.class)
public class ProjectRepositoryIT {

    private static final String ID_0 = "id-0";
    private static final String ID_1 = "id-1";
    private static final String ID_2 = "id-2";
    private static final String ID_3 = "id-3";
    private static final String ID_4 = "id-4";

    private static final String NAME_0 = "name-0";
    private static final String NAME_1 = "name-1";
    private static final String NAME_2 = "name-2";
    private static final String NAME_3 = "name-3";
    private static final String FAKE_NAME = "fake-name";

    private static final String CREATOR_0 = "creator-0";
    private static final String CREATOR_1 = "creator-1";
    private static final String CREATOR_2 = "creator-2";
    private static final String CREATOR_3 = "creator-3";
    private static final String FAKE_CREATOR = "fake-creator";

    private static final Long STAR_COUNT_MIN = -1L;
    private static final Long STAR_COUNT_0 = 0L;
    private static final Long STAR_COUNT_1 = 1L;
    private static final Long STAR_COUNT_2 = 2L;
    private static final Long STAR_COUNT_3 = 3L;
    private static final Long STAR_COUNT_MAX = 100L;

    private static final Long FORK_COUNT_0 = 0L;
    private static final Long FORK_COUNT_1 = 1L;
    private static final Long FORK_COUNT_2 = 2L;
    private static final Long FORK_COUNT_3 = 3L;
    private static final Long FAKE_COUNT = 123234L;
    private static final Long FORK_COUNT_MAX = 100L;

    private static final Project PROJECT_0 = new Project(ID_0, NAME_0, CREATOR_0, true, STAR_COUNT_0, FORK_COUNT_0);
    private static final Project PROJECT_1 = new Project(ID_1, NAME_1, CREATOR_1, true, STAR_COUNT_1, FORK_COUNT_1);
    private static final Project PROJECT_2 = new Project(ID_2, NAME_2, CREATOR_2, true, STAR_COUNT_2, FORK_COUNT_2);
    private static final Project PROJECT_3 = new Project(ID_3, NAME_3, CREATOR_3, true, STAR_COUNT_3, FORK_COUNT_3);
    private static final Project PROJECT_4 = new Project(ID_4, NAME_0, CREATOR_0, false, STAR_COUNT_0, FORK_COUNT_0);

    private static final List<Project> PROJECTS = Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_2, PROJECT_3, PROJECT_4);

    private static final CosmosEntityInformation<Project, String> entityInformation =
            new CosmosEntityInformation<>(Project.class);

    private static CosmosTemplate staticTemplate;
    private static boolean isSetupDone;

    @Autowired
    private CosmosTemplate template;

    @Autowired
    private ProjectRepository repository;

    @Before
    public void setUp() {
        if (!isSetupDone) {
            staticTemplate = template;
            template.createContainerIfNotExists(entityInformation);
        }
        this.repository.saveAll(PROJECTS);
        isSetupDone = true;
    }

    @After
    public void cleanup() {
        this.repository.deleteAll();
    }

    @AfterClass
    public static void afterClassCleanup() {
        staticTemplate.deleteContainer(entityInformation.getContainerName());
    }

    private void assertProjectListEquals(@NonNull List<Project> projects, @NonNull List<Project> reference) {
        Assert.assertEquals(reference.size(), projects.size());

        projects.sort(Comparator.comparing(Project::getId));
        reference.sort(Comparator.comparing(Project::getId));

        Assert.assertEquals(reference, projects);
    }

    @Test
    public void testFindByWithAnd() {
        List<Project> projects = this.repository.findByNameAndStarCount(NAME_1, STAR_COUNT_1);

        assertProjectListEquals(projects, Collections.singletonList(PROJECT_1));

        projects = this.repository.findByNameAndStarCount(NAME_0, STAR_COUNT_1);

        Assert.assertTrue(projects.isEmpty());

        projects = this.repository.findByNameAndStarCount(NAME_0, STAR_COUNT_0);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_4));
    }

    @Test
    public void testFindByWithOr() {
        List<Project> projects = this.repository.findByNameOrForkCount(NAME_2, STAR_COUNT_2);

        assertProjectListEquals(projects, Collections.singletonList(PROJECT_2));

        projects = this.repository.findByNameOrForkCount(FAKE_NAME, FAKE_COUNT);

        Assert.assertTrue(projects.isEmpty());

        projects = this.repository.findByNameOrForkCount(NAME_0, FORK_COUNT_1);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_4));
    }

    @Test
    public void testFindByWithAndPartition() {
        List<Project> projects = this.repository.findByNameAndCreator(NAME_1, CREATOR_1);

        assertProjectListEquals(projects, Collections.singletonList(PROJECT_1));

        projects = this.repository.findByNameAndCreator(NAME_0, CREATOR_1);

        Assert.assertTrue(projects.isEmpty());

        projects = this.repository.findByNameAndCreator(NAME_0, CREATOR_0);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_4));
    }

    @Test
    public void testFindByWithOrPartition() {
        List<Project> projects = this.repository.findByNameOrCreator(NAME_2, CREATOR_2);

        assertProjectListEquals(projects, Collections.singletonList(PROJECT_2));

        projects = this.repository.findByNameOrCreator(FAKE_NAME, FAKE_CREATOR);

        Assert.assertTrue(projects.isEmpty());

        projects = this.repository.findByNameOrCreator(NAME_0, CREATOR_1);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_4));
    }

    @Test
    public void testFindByWithAndOr() {
        List<Project> projects = repository.findByNameAndCreatorOrForkCount(NAME_0, CREATOR_1, FORK_COUNT_2);

        assertProjectListEquals(projects, Collections.singletonList(PROJECT_2));

        projects = repository.findByNameAndCreatorOrForkCount(NAME_1, CREATOR_2, FAKE_COUNT);

        Assert.assertTrue(projects.isEmpty());

        projects = repository.findByNameAndCreatorOrForkCount(NAME_1, CREATOR_1, FORK_COUNT_2);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_1, PROJECT_2));
    }

    @Test
    public void testFindByWithOrAnd() {
        List<Project> projects = repository.findByNameOrCreatorAndForkCount(NAME_0, CREATOR_1, FORK_COUNT_2);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_4));

        projects = repository.findByNameOrCreatorAndForkCount(FAKE_NAME, CREATOR_1, FORK_COUNT_2);

        Assert.assertTrue(projects.isEmpty());

        projects = repository.findByNameOrCreatorAndForkCount(NAME_1, CREATOR_2, FORK_COUNT_2);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_1, PROJECT_2));
    }

    @Test
    public void testFindByWithOrOr() {
        List<Project> projects = repository.findByNameOrCreatorOrForkCount(NAME_0, CREATOR_1, FORK_COUNT_2);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_2, PROJECT_4));

        projects = repository.findByNameOrCreatorOrForkCount(FAKE_NAME, FAKE_CREATOR, FAKE_COUNT);

        Assert.assertTrue(projects.isEmpty());
    }

    @Test
    public void testFindByWithOrAndOr() {
        List<Project> projects = repository.findByNameOrCreatorAndForkCountOrStarCount(NAME_1, CREATOR_0,
                FORK_COUNT_2, STAR_COUNT_3);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_1, PROJECT_3));

        projects = repository.findByNameOrCreatorAndForkCountOrStarCount(NAME_1, CREATOR_0, FORK_COUNT_0, STAR_COUNT_3);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_3, PROJECT_4));

        projects = repository.findByNameOrCreatorAndForkCountOrStarCount(FAKE_NAME, CREATOR_1,
                FORK_COUNT_0, FAKE_COUNT);

        Assert.assertTrue(projects.isEmpty());
    }

    @Test
    public void testFindByGreaterThan() {
        List<Project> projects = repository.findByForkCountGreaterThan(FORK_COUNT_1);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_2, PROJECT_3));

        projects = repository.findByForkCountGreaterThan(FAKE_COUNT);

        Assert.assertTrue(projects.isEmpty());
    }

    @Test
    public void testFindByGreaterThanWithAndOr() {
        List<Project> projects = repository.findByCreatorAndForkCountGreaterThan(CREATOR_2, FORK_COUNT_1);

        assertProjectListEquals(projects, Collections.singletonList(PROJECT_2));

        projects = repository.findByCreatorAndForkCountGreaterThan(CREATOR_0, FORK_COUNT_1);

        Assert.assertTrue(projects.isEmpty());

        projects = repository.findByCreatorOrForkCountGreaterThan(CREATOR_0, FORK_COUNT_2);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_3, PROJECT_4));
    }

    @Test
    public void testFindByLessThan() {
        List<Project> projects = repository.findByStarCountLessThan(STAR_COUNT_0);

        Assert.assertTrue(projects.isEmpty());

        projects = repository.findByStarCountLessThan(STAR_COUNT_2);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_4));
    }

    @Test
    public void testFindByLessThanEqual() {
        List<Project> projects = repository.findByForkCountLessThanEqual(STAR_COUNT_MIN);

        Assert.assertTrue(projects.isEmpty());

        projects = repository.findByForkCountLessThanEqual(STAR_COUNT_2);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_2, PROJECT_4));
    }

    @Test
    public void testFindByLessThanAndGreaterThan() {
        List<Project> projects = repository.findByStarCountLessThanAndForkCountGreaterThan(STAR_COUNT_0, FORK_COUNT_3);

        Assert.assertTrue(projects.isEmpty());

        projects = repository.findByStarCountLessThanAndForkCountGreaterThan(STAR_COUNT_3, FORK_COUNT_0);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_1, PROJECT_2));
    }

    @Test
    public void testFindByLessThanEqualsAndGreaterThanEquals() {
        List<Project> projects = repository.findByForkCountLessThanEqualAndStarCountGreaterThan(
                STAR_COUNT_MIN, FORK_COUNT_0);

        Assert.assertTrue(projects.isEmpty());

        projects = repository.findByForkCountLessThanEqualAndStarCountGreaterThan(STAR_COUNT_3, FORK_COUNT_0);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_1, PROJECT_2, PROJECT_3));
    }

    @Test
    public void testFindByGreaterThanEqual() {
        List<Project> projects = repository.findByStarCountGreaterThanEqual(STAR_COUNT_MAX);

        Assert.assertTrue(projects.isEmpty());

        projects = repository.findByStarCountGreaterThanEqual(STAR_COUNT_2);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_2, PROJECT_3));
    }

    @Test
    public void testFindByGreaterThanEqualAnd() {
        List<Project> projects = repository.findByForkCountGreaterThanEqualAndCreator(FORK_COUNT_MAX, CREATOR_2);

        Assert.assertTrue(projects.isEmpty());

        projects = repository.findByForkCountGreaterThanEqualAndCreator(FORK_COUNT_0, CREATOR_0);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_4));
    }

    @Test
    public void testFindByTrue() {
        final List<Project> projects = repository.findByHasReleasedTrue();

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_2, PROJECT_3));
    }

    @Test
    public void testFindByFalse() {
        final List<Project> projects = repository.findByHasReleasedFalse();

        assertProjectListEquals(projects, Arrays.asList(PROJECT_4));
    }

    @Test
    public void testFindByTrueFalseWithAnd() {
        List<Project> projects = repository.findByHasReleasedTrueAndCreator(CREATOR_3);
        assertProjectListEquals(projects, Arrays.asList(PROJECT_3));

        projects = repository.findByHasReleasedFalseAndCreator(CREATOR_3);
        assertProjectListEquals(projects, Arrays.asList());
    }

    @Test
    public void testFindByTrueFalseWithOr() {
        List<Project> projects = repository.findByHasReleasedTrueOrCreator(CREATOR_0);
        assertProjectListEquals(projects, PROJECTS);

        projects = repository.findByHasReleasedFalseOrCreator(CREATOR_3);
        assertProjectListEquals(projects, Arrays.asList(PROJECT_3, PROJECT_4));
    }

    @Test
    public void findByIdWithPartitionKey() {
        final Optional<Project> project = repository.findById(PROJECT_0.getId(),
            new PartitionKey(entityInformation.getPartitionKeyFieldValue(PROJECT_0)));

        Assert.assertTrue(project.isPresent());

        Assert.assertEquals(project.get(), PROJECT_0);
    }

    @Test
    public void findByIdWithPartitionKeyNotFound() {
        final Optional<Project> project = repository.findById("unknown-id",
            new PartitionKey("unknown-partition-key"));

        Assert.assertFalse(project.isPresent());
    }


    @Test
    public void testFindByIn() {
        List<Project> projects = repository.findByCreatorIn(Collections.singleton(FAKE_CREATOR));

        Assert.assertTrue(projects.isEmpty());

        projects = repository.findByCreatorIn(Arrays.asList(CREATOR_1, CREATOR_2));

        assertProjectListEquals(projects, Arrays.asList(PROJECT_1, PROJECT_2));

        projects = repository.findByCreatorIn(Arrays.asList(CREATOR_0, FAKE_CREATOR));

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_4));
    }

    @Test
    public void testFindByInWithAnd() {
        List<Project> projects = repository.findByCreatorInAndStarCountIn(Arrays.asList(CREATOR_0, CREATOR_1),
                Arrays.asList(STAR_COUNT_2, STAR_COUNT_3));

        Assert.assertTrue(projects.isEmpty());

        projects = repository.findByCreatorInAndStarCountIn(Arrays.asList(CREATOR_0, CREATOR_1),
                Arrays.asList(STAR_COUNT_0, STAR_COUNT_2));

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_4));

        projects = repository.findByCreatorInAndStarCountIn(Arrays.asList(CREATOR_0, CREATOR_1, CREATOR_2),
                Arrays.asList(STAR_COUNT_0, STAR_COUNT_1, STAR_COUNT_2));

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_2, PROJECT_4));
    }

    @Test
    public void testFindByNotIn() {
        List<Project> projects = repository.findByCreatorNotIn(
                Arrays.asList(CREATOR_0, CREATOR_1, CREATOR_2, CREATOR_3));

        Assert.assertTrue(projects.isEmpty());

        projects = repository.findByCreatorNotIn(Arrays.asList(CREATOR_1, CREATOR_2));

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_3, PROJECT_4));

        projects = repository.findByCreatorNotIn(Arrays.asList(CREATOR_0, FAKE_CREATOR));

        assertProjectListEquals(projects, Arrays.asList(PROJECT_1, PROJECT_2, PROJECT_3));
    }

    @Test
    public void testFindByInWithNotIn() {
        List<Project> projects = repository.findByCreatorInAndStarCountNotIn(Collections.singletonList(FAKE_CREATOR),
                Arrays.asList(STAR_COUNT_2, STAR_COUNT_3));

        Assert.assertTrue(projects.isEmpty());

        projects = repository.findByCreatorInAndStarCountNotIn(Arrays.asList(CREATOR_0, CREATOR_1),
                Arrays.asList(STAR_COUNT_0, STAR_COUNT_2));

        assertProjectListEquals(projects, Collections.singletonList(PROJECT_1));

        projects = repository.findByCreatorInAndStarCountNotIn(Arrays.asList(CREATOR_0, CREATOR_1, CREATOR_2),
                Arrays.asList(STAR_COUNT_1, STAR_COUNT_2));

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_4));
    }

    @Test
    public void testFindByNameIsNull() {
        List<Project> projects = repository.findByNameIsNull();

        Assert.assertTrue(projects.isEmpty());

        final Project nullNameProject = new Project("id-999", null, CREATOR_0, true, STAR_COUNT_0,
                FORK_COUNT_0);

        this.repository.save(nullNameProject);
        projects = repository.findByNameIsNull();

        assertProjectListEquals(projects, Collections.singletonList(nullNameProject));
    }

    @Test
    public void testFindByNameIsNotNull() {
        List<Project> projects = repository.findByNameIsNotNull();

        assertProjectListEquals(projects, PROJECTS);

        this.repository.deleteAll();
        this.repository.save(new Project("id-999", null, CREATOR_0, true, STAR_COUNT_0, FORK_COUNT_0));

        projects = repository.findByNameIsNotNull();

        Assert.assertTrue(projects.isEmpty());
    }

    @Test
    public void testFindByNameIsNullWithAnd() {
        List<Project> projects = repository.findByNameIsNullAndForkCount(FORK_COUNT_MAX);

        Assert.assertTrue(projects.isEmpty());

        final Project nullNameProject = new Project("id-999", null, CREATOR_0, true, STAR_COUNT_0,
                FORK_COUNT_0);

        this.repository.save(nullNameProject);
        projects = repository.findByNameIsNullAndForkCount(FORK_COUNT_0);

        assertProjectListEquals(projects, Collections.singletonList(nullNameProject));
    }

    @Test
    public void testFindByNameIsNotNullWithAnd() {
        List<Project> projects = repository.findByNameIsNotNullAndHasReleased(true);

        assertProjectListEquals(projects, Arrays.asList(PROJECT_0, PROJECT_1, PROJECT_2, PROJECT_3));

        this.repository.deleteAll();
        this.repository.save(new Project("id-999", null, CREATOR_0, true, STAR_COUNT_0, FORK_COUNT_0));

        projects = repository.findByNameIsNotNullAndHasReleased(true);
        Assert.assertTrue(projects.isEmpty());
    }

    @Test
    public void testFindAllByPartitionKey() {
        List<Project> findAll =
            repository.findAll(new PartitionKey(CREATOR_0));
        //  Since there are two projects with creator_0
        assertThat(findAll.size()).isEqualTo(2);
        assertThat(findAll.containsAll(Lists.newArrayList(PROJECT_0, PROJECT_4))).isTrue();

        findAll = repository.findAll(new PartitionKey(CREATOR_1));
        //  Since there is one projects with creator_1
        assertThat(findAll.size()).isEqualTo(1);
        assertThat(findAll.contains(PROJECT_1)).isTrue();


        findAll = repository.findAll(new PartitionKey(CREATOR_2));
        //  Since there is one projects with creator_2
        assertThat(findAll.size()).isEqualTo(1);
        assertThat(findAll.contains(PROJECT_2)).isTrue();

        findAll = repository.findAll(new PartitionKey(CREATOR_3));
        //  Since there is one projects with creator_3
        assertThat(findAll.size()).isEqualTo(1);
        assertThat(findAll.contains(PROJECT_3)).isTrue();
    }
}
