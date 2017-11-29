package com.thinkbiganalytics.projects.controllers;

/*-
 * #%L
 * kylo-project-manager-service
 * %%
 * Copyright (C) 2017 ThinkBig Analytics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.thinkbiganalytics.metadata.api.project.security.ProjectAccessControl;
import com.thinkbiganalytics.metadata.rest.model.Project;
import com.thinkbiganalytics.projects.security.ProjectSecurityService;
import com.thinkbiganalytics.projects.services.ProjectService;
import com.thinkbiganalytics.projects.services.impl.ProjectServiceImpl;
import com.thinkbiganalytics.rest.model.RestResponseStatus;
import com.thinkbiganalytics.security.rest.controller.SecurityModelTransform;
import com.thinkbiganalytics.security.rest.model.ActionGroup;
import com.thinkbiganalytics.security.rest.model.PermissionsChange;
import com.thinkbiganalytics.security.rest.model.RoleMembershipChange;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

@Api(tags = "Project Manager - Projects", produces = "application/json")
@Path(ProjectController.BASE)
@SwaggerDefinition(tags = @Tag(name = "Project Manager - Projects", description = "manages projects"))
//@Component
public class ProjectController {

    public static final String BASE = "/v1/projectManager/projects";
    public static Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);
    @Inject
    private ProjectService projectService;

    @Inject
    private ProjectSecurityService projectSecurityService;

    @Inject
    private SecurityModelTransform securityTransform;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the list of projects.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the list of projects.", response = Project.class, responseContainer = "List")
    )
    public Response getProjects() {
        Collection<Project> projects = projectService.getProjects();
        return Response.ok(projects).build();
    }


    /**
     * Creates a project
     *
     * @param project the project definition
     * @return true on success
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Creates a project.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the project.", response = Boolean.class)
    )
    @Nonnull
    public Response createProject(@Nonnull final Project project) {
        projectService.createProject(project);

        return Response.ok(Boolean.TRUE).build();
    }

    /**
     * Updates a project
     *
     * @param project the project definition
     * @return true on success
     */
    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Updates a project.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the project.", response = Boolean.class)
    )
    @Nonnull
    public Response updateProject(@Nonnull final Project project) {
        projectService.updateProject(project);

        return Response.ok(Boolean.TRUE).build();
    }

    /**
     * Deletes a project
     *
     * @param id the project id
     * @return true on success
     */
    @DELETE
    @Path("{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Deletes a project.")
    @ApiResponses(
        @ApiResponse(code = 200, message = "Returns the project.")
    )
    @Nonnull
    public Response deleteProject(@PathParam("projectId") String projectIdStr) {
        // TODO: need to throw 404 if project does not exist
        // TODO: project details page should redirect to projects list after project has been deleted
        projectService.deleteProject(projectIdStr);

        return Response.ok().build();
    }

    @GET
    @Path("{projectId}/actions/available")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the list of available actions that may be permitted or revoked on a project.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the actions.", response = ActionGroup.class),
                      @ApiResponse(code = 404, message = "A project with the given ID does not exist.", response = RestResponseStatus.class)
                  })
    public Response getAvailableActions(@PathParam("projectId") String projectIdStr) {
        log.debug("Get available actions for project: {}", projectIdStr);

        return this.projectSecurityService.getAvailableProjectActions(projectIdStr)
            .map(g -> Response.ok(g).build())
            .orElseThrow(() -> new WebApplicationException("A project with the given ID does not exist: " + projectIdStr, Response.Status.NOT_FOUND));
    }

    @GET
    @Path("{projectId}/actions/allowed")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the list of actions permitted for the given username and/or groups.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the actions.", response = ActionGroup.class),
                      @ApiResponse(code = 404, message = "A project with the given ID does not exist.", response = RestResponseStatus.class)
                  })
    public Response getAllowedActions(@PathParam("projectId") String projectIdStr,
                                      @QueryParam("user") Set<String> userNames,
                                      @QueryParam("group") Set<String> groupNames) {
        log.debug("Get allowed actions for project: {}", projectIdStr);

        Set<? extends Principal> users = Arrays.stream(this.securityTransform.asUserPrincipals(userNames)).collect(Collectors.toSet());
        Set<? extends Principal> groups = Arrays.stream(this.securityTransform.asGroupPrincipals(groupNames)).collect(Collectors.toSet());

        return this.projectSecurityService.getAllowedProjectActions(projectIdStr, Stream.concat(users.stream(), groups.stream()).collect(Collectors.toSet()))
            .map(g -> Response.ok(g).build())
            .orElseThrow(() -> new WebApplicationException("A project with the given ID does not exist: " + projectIdStr, Status.NOT_FOUND));
    }

    @POST
    @Path("{projectId}/actions/allowed")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Updates the permissions for a project using the supplied permission change request.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "The permissions were changed successfully.", response = ActionGroup.class),
                      @ApiResponse(code = 400, message = "The type is not valid.", response = RestResponseStatus.class),
                      @ApiResponse(code = 404, message = "No project exists with the specified ID.", response = RestResponseStatus.class)
                  })
    public Response postPermissionsChange(@PathParam("projectId") String projectIdStr,
                                          PermissionsChange changes) {
        return this.projectSecurityService.changeProjectPermissions(projectIdStr, changes)
            .map(g -> Response.ok(g).build())
            .orElseThrow(() -> new WebApplicationException("A project with the given ID does not exist: " + projectIdStr, Response.Status.NOT_FOUND));
    }

    @GET
    @Path("{projectId}/actions/change")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Constructs and returns a permission change request for a set of users/groups containing the actions that the requester may permit or revoke.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the change request that may be modified by the client and re-posted.", response = PermissionsChange.class),
                      @ApiResponse(code = 400, message = "The type is not valid.", response = RestResponseStatus.class),
                      @ApiResponse(code = 404, message = "No project exists with the specified ID.", response = RestResponseStatus.class)
                  })
    public Response getAllowedPermissionsChange(@PathParam("projectId") String projectIdStr,
                                                @QueryParam("type") String changeType,
                                                @QueryParam("user") Set<String> userNames,
                                                @QueryParam("group") Set<String> groupNames) {
        if (StringUtils.isBlank(changeType)) {
            throw new WebApplicationException("The query parameter \"type\" is required", Status.BAD_REQUEST);
        }

        Set<? extends Principal> users = Arrays.stream(this.securityTransform.asUserPrincipals(userNames)).collect(Collectors.toSet());
        Set<? extends Principal> groups = Arrays.stream(this.securityTransform.asGroupPrincipals(groupNames)).collect(Collectors.toSet());

        return this.projectSecurityService.createProjectPermissionChange(projectIdStr,
                                                                         PermissionsChange.ChangeType.valueOf(changeType.toUpperCase()),
                                                                         Stream.concat(users.stream(), groups.stream()).collect(Collectors.toSet()))
            .map(p -> Response.ok(p).build())
            .orElseThrow(() -> new WebApplicationException("A project with the given ID does not exist: " + projectIdStr, Status.NOT_FOUND));
    }

    @GET
    @Path("{projectId}/roles")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Gets the list of assigned members the project's roles")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "Returns the role memberships.", response = ActionGroup.class),
                      @ApiResponse(code = 404, message = "A project with the given ID does not exist.", response = RestResponseStatus.class)
                  })
    public Response getRoleMemberships(@PathParam("projectId") String projectIdStr,
                                       @QueryParam("verbose") @DefaultValue("false") boolean verbose) {

        return this.projectSecurityService.getProjectRoleMemberships(projectIdStr)
            .map(m -> {
                log.debug("RoleMemberships m = {}", m);
                return Response.ok(m).build();
            })
            .orElseThrow(() -> new WebApplicationException("A project with the given ID does not exist: " + projectIdStr, Status.NOT_FOUND));

    }


    @POST
    @Path("{projectId}/roles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation("Updates the members of one of a project's roles.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "The permissions were changed successfully.", response = ActionGroup.class),
                      @ApiResponse(code = 404, message = "No project exists with the specified ID.", response = RestResponseStatus.class)
                  })
    public Response postRoleMembershipChange(@PathParam("projectId") String projectIdStr,
                                             RoleMembershipChange changes) {

        log.debug("RoleMembershipChanges = '{}'", changes.getUsers());

        return this.projectSecurityService.changeProjectRoleMemberships(projectIdStr, changes)
            .map(m -> Response.ok(m).build())
            .orElseThrow(() -> new WebApplicationException("Either a project with the ID \"" + projectIdStr
                                                           + "\" does not exist or it does not have a role named \""
                                                           + changes.getRoleName() + "\"", Status.NOT_FOUND));
    }


    @GET
    @Path("{projectId}/roles/change")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoleMemberships(@PathParam("projectId") String projectIdStr) {
        RoleMembershipChange change = this.projectSecurityService.getProjectRoleMemberships(projectIdStr)
            .map(membships -> membships.getAssigned().values().stream().findAny()
                .map(membership -> new RoleMembershipChange(RoleMembershipChange.ChangeType.REPLACE, membership))
                .orElse(new RoleMembershipChange(RoleMembershipChange.ChangeType.REPLACE, ProjectAccessControl.ROLE_READER)))
            .orElseThrow(() -> new WebApplicationException("A project with the given ID does not exist: " + projectIdStr, Status.NOT_FOUND));

        return Response.ok(change).build();
    }

}
