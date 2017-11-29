define(['angular','feed-mgr/module-name','constants/AccessConstants'], function (angular,moduleName) {
    angular.module(moduleName).factory('ProjectsService',["$q","$http","RestUrlService","AccessControlService","EntityAccessControlService", function ($q, $http, RestUrlService,AccessControlService,EntityAccessControlService) {

        /**
         * internal cache of projects
         * @type {Array}
         */
        var projects = [];
        /**
         * the loading request for all projects
         * @type promise
         */
        var loadingRequest = null;

        var loading = false;
        /**
         * Create filter function for a query string
         */
        function createFilterFor(query) {
            var lowercaseQuery = angular.lowercase(query);
            return function filterFn(tag) {
                return (tag._lowername.indexOf(lowercaseQuery) === 0);
            };
        }


        function loadAll() {
            if (!loading) {
                loading = true;
                loadingRequest = $http.get(RestUrlService.PROJECTS_URL).then(function (response) {
                    loading = false;
                    loadingRequest = null;
                    projects = response.data.map(function (project) {
                        project._lowername = project.projectName.toLowerCase();
                        /*
                        project.createFeed = false;
                        //if under entity access control we need to check if the user has the "CREATE_FEED" permission associated with the selected project.
                        //if the user doesnt have this permission they cannot create feeds under this project
                        if (AccessControlService.isEntityAccessControlled()) {
                            if (AccessControlService.hasEntityAccess(EntityAccessControlService.ENTITY_ACCESS.PROJECT.CREATE_FEED, project, "project")) {
                                project.createFeed = true;
                            }
                        }
                        else {
                            project.createFeed = true;
                        }
                        */
                        
                        return project;
                    });
                    return projects;
                }, function (err) {
                    loading = false;
                });
                return loadingRequest;
            }
            else {
                if (loadingRequest != null) {
                    return loadingRequest;
                }
                else {
                    var defer = $q.defer();
                    defer.resolve(projects);
                    return defer.promise;
                }
            }
        }

            /**
             * A project for grouping similar kylo concepts.
             *
             * @typedef {Object} ProjectModel
             * @property {string|null} id the unique identifier
             * @property {string|null} projectName a human-readable name
             * @property {string|null} description a sentence describing the project
             * @property {string|null} icon the name of a Material Design icon
             * @property {string|null} iconColor the color of the icon
             */

            /**
             * Utility functions for managing projects.
             *
             * @type {Object}
             */
            var data = {
                /**
                 * Global project data used across directives.
                 *
                 * @type {ProjectModel}
                 */
                model: {},

                init: function () {
                    this.reload();
                },
                reload: function () {
                    var self = this;
                    return loadAll().then(function (projects) {
                        return self.projects = projects;
                    }, function (err) {
                    });
                },
                /**
                 * Adds/updates the project back to the cached list.
                 * returns true if successful, false if not
                 * @param savedProject
                 * @return {boolean}
                 */
                update:function(savedProject){
                    var self = this;
                    if(angular.isDefined(savedProject.id)) {
                        var project = _.find(self.projects, function (project) {
                            return project.id == savedProject.id;
                        });
                        savedProject._lowername = savedProject.projectName.toLowerCase();
                        /*
                        savedProject.createFeed = false;
                        //if under entity access control we need to check if the user has the "CREATE_FEED" permission associated with the selected project.
                        //if the user doesnt have this permission they cannot create feeds under this project
                        if (AccessControlService.isEntityAccessControlled()) {
                            if (AccessControlService.hasEntityAccess(EntityAccessControlService.ENTITY_ACCESS.PROJECT.CREATE_FEED, savedProject, "project")) {
                                savedProject.createFeed = true;
                            }
                        }
                        else {
                            savedProject.createFeed = true;
                        }
                        */

                        if(angular.isDefined(project)) {
                          var idx = _.indexOf(self.projects, project);
                            self.projects[idx] = savedProject;
                        }
                        else {
                            self.projects.push(savedProject);
                        }
                        return true;
                    }
                    else {
                        self.reload();
                    }
                    return false;
                },
                delete: function (project) {
                    var promise = $http({
                        url: RestUrlService.PROJECTS_URL + "/" + project.id,
                        method: "DELETE"
                    });
                    return promise;
                },
                save: function (project) {
                    //prepare access control changes if any
                    EntityAccessControlService.updateRoleMembershipsForSave(project.roleMemberships);
                    //EntityAccessControlService.updateRoleMembershipsForSave(project.feedRoleMemberships);

                    var promise = $http({
                        url: RestUrlService.PROJECTS_URL,
                        method: "POST",
                        data: angular.toJson(project),
                        headers: {
                            'Content-Type': 'application/json; charset=UTF-8'
                        }
                    });
                    return promise;
                },
                doUpdate: function (project) {
                    //prepare access control changes if any
                    EntityAccessControlService.updateRoleMembershipsForSave(project.roleMemberships);

                    var promise = $http({
                        url: RestUrlService.PROJECTS_URL,
                        method: "PUT",
                        data: angular.toJson(project),
                        headers: {
                            'Content-Type': 'application/json; charset=UTF-8'
                        }
                    });
                    return promise;
                },
                findProject: function (id) {

                    var self = this;
                    var project = _.find(self.projects, function (project) {
                        return project.id == id;
                    });
                    return project;
                },
                /*findProjectByName: function (name) {
                    if (name != undefined) {
                        var self = this;
                        var project = _.find(self.projects, function (project) {
                            return project.projectName.toLowerCase() == name.toLowerCase();
                        });
                        return project;
                    }
                    return null;
                },*/
                findProjectBySystemName: function (systemName) {
                  if (systemName != undefined) {
                      var self = this;
                      var project = _.find(self.projects, function (project) {
                          return project.systemName == systemName;
                      });
                      return project;
                  }
                  return null;
                },
                getProjectBySystemName:function(systemName){
                    var self = this;
                    var deferred = $q.defer();
                    var projectCache = self.findProjectBySystemName(systemName);
                    if(typeof projectCache === 'undefined' || projectCache === null) {
                        $http.get(RestUrlService.PROJECT_DETAILS_BY_SYSTEM_NAME_URL(systemName))
                            .then(function (response) {
                                var projectResponse = response.data;
                                deferred.resolve(projectResponse);
                            });
                    }
                    else {
                        deferred.resolve(projectCache);
                    }
                    return deferred.promise;
                },
                getProjectById:function(projectId) {
                    var deferred = $q.defer();
                    $http.get(RestUrlService.PROJECT_DETAILS_BY_ID_URL(projectId))
                        .then(function(response) {
                            var projectResponse = response.data;
                            return deferred.resolve(projectResponse);
                        });
                    return deferred.promise;
                },
                projects: [],
                querySearch: function (query) {

                    var self = this;
                    var deferred = $q.defer();
                    if (self.projects.length == 0) {
                        loadAll().then(function (response) {
                            data.loading = false;
                            if (query) {
                                var results = response.filter(createFilterFor(query))
                                deferred.resolve(results);
                            }
                            else {
                                deferred.resolve(response);
                            }
                        }, function (err) {
                            data.loading = false;
                        });
                    }
                    else {
                        var results = query ? self.projects.filter(createFilterFor(query)) : self.projects;
                        deferred.resolve(results);
                    }
                    return deferred.promise;

                },

                /**
                 * Creates a new project model.
                 *
                 * @returns {ProjectModel} the new project model
                 */
                newProject: function () {
                    return {
                        id: null,
                        projectName: null,
                        description: null,
                        icon: null, iconColor: null,
                        userProperties: [],
                        roleMemberships: [],
                        notebookFolderEnabled: false,
                        owner: null
                    };
                },

                /**
                 * Gets the user fields for a new project.
                 *
                 * @returns {Promise} for the user fields
                 */
                getUserFields: function () {
                    return $http.get(RestUrlService.GET_PROJECT_USER_FIELD_URL)
                        .then(function (response) {
                            return response.data;
                        });
                },
                /**
                 * check if the user has access on an entity
                 * @param permissionsToCheck an Array or a single string of a permission/action to check against this entity and current user
                 * @param entity the entity to check. if its undefined it will use the current project in the model
                 * @returns {*} a promise, or a true/false.  be sure to wrap this with a $q().then()
                 */
                hasEntityAccess: function (permissionsToCheck, entity) {
                    if (entity == undefined) {
                        entity = data.model;
                    }
                    return AccessControlService.hasEntityAccess(permissionsToCheck, entity, EntityAccessControlService.entityRoleTypes.PROJECT);
                }
            };

            //EntityAccessControlService.ENTITY_ACCESS.CHANGE_PROJECT_PERMISSIONS
            //data.init();
            return data;

    }]);
});