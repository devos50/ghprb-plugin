package org.jenkinsci.plugins.ghprb;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

import java.util.Map;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.plugins.git.GitSCM;

import org.joda.time.DateTime;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.kohsuke.github.GHIssueState;

import com.coravy.hudson.plugins.github.GithubProjectProperty;

/**
 * @author mdelapenya (Manuel de la Peña)
 */
public abstract class GhprbITBaseTestCase {

    @Mock
    protected GHCommitPointer commitPointer;
    @Mock
    protected GHPullRequest ghPullRequest;
    @Mock
    protected GhprbGitHub ghprbGitHub;
    @Mock
    protected GHRepository ghRepository;
    @Mock
    protected GHUser ghUser;
    @Mock
    protected GitHub gitHub;
    @Mock
    protected Ghprb helper;
    
    
    protected GhprbBuilds builds;
    
    protected GhprbTrigger trigger;

    // Stubs
    protected GHRateLimit ghRateLimit = new GHRateLimit();

    protected void beforeTest(Map<String, Object> globalConfig, Map<String, Object> triggerConfig, AbstractProject<?, ?> project) throws Exception {
        project.addProperty(new GithubProjectProperty("https://github.com/user/dropwizard"));
        GhprbTestUtil.setupGhprbTriggerDescriptor(globalConfig);
        
        trigger = GhprbTestUtil.getTrigger(triggerConfig);
        
        given(ghprbGitHub.get()).willReturn(gitHub);
        
        given(gitHub.getRateLimit()).willReturn(ghRateLimit);
        given(gitHub.getRepository(anyString())).willReturn(ghRepository);
        
        given(ghPullRequest.getHead()).willReturn(commitPointer);
        given(ghPullRequest.getUser()).willReturn(ghUser);
        
        given(commitPointer.getRef()).willReturn("ref");
        given(commitPointer.getSha()).willReturn("sha");
        
        given(ghRepository.getName()).willReturn("dropwizard");

        GhprbTestUtil.mockPR(ghPullRequest, commitPointer, new DateTime(), new DateTime().plusDays(1));
        

        given(ghRepository.getPullRequests(eq(GHIssueState.OPEN))).willReturn(newArrayList(ghPullRequest)).willReturn(newArrayList(ghPullRequest));

        given(ghUser.getEmail()).willReturn("email@email.com");
        given(ghUser.getLogin()).willReturn("user");

        ghRateLimit.remaining = GhprbTestUtil.INITIAL_RATE_LIMIT;

        GhprbTestUtil.mockCommitList(ghPullRequest);
        

        GhprbRepository repo = Mockito.spy(new GhprbRepository("user", "dropwizard", helper));
        Mockito.doReturn(ghRepository).when(repo).getGitHubRepo();
        Mockito.doNothing().when(repo).addComment(Mockito.anyInt(), Mockito.anyString(), any(AbstractBuild.class), any(TaskListener.class));
        

        builds = new GhprbBuilds(trigger, repo);

        // Creating spy on ghprb, configuring repo
        given(helper.getGitHub()).willReturn(ghprbGitHub);
        given(helper.getRepository()).willReturn(repo);
        given(helper.getTrigger()).willReturn(trigger);
        given(helper.isWhitelisted(ghUser)).willReturn(true);
        given(helper.getBuilds()).willReturn(builds);
        

        Mockito.doCallRealMethod().when(helper).run();


        // Configuring and adding Ghprb trigger
        project.addTrigger(trigger);

        // Configuring Git SCM
        GitSCM scm = GhprbTestUtil.provideGitSCM();
        project.setScm(scm);

        trigger.start(project, true);
        trigger.setHelper(helper);
    }

}