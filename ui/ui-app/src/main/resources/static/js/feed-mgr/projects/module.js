define(['angular','feed-mgr/projects/module-name','kylo-utils/LazyLoadUtil','constants/AccessConstants','app','@uirouter/angularjs','kylo-feedmgr'], function (angular,moduleName,lazyLoadUtil,AccessConstants) {
    //LAZY LOADED into the application
    var module = angular.module(moduleName, ['ui.router']);

    module.config(['$stateProvider','$compileProvider',function ($stateProvider,$compileProvider) {
        //preassign modules until directives are rewritten to use the $onInit method.
        //https://docs.angularjs.org/guide/migration#migrating-from-1-5-to-1-6
        $compileProvider.preAssignBindingsEnabled(true);

        $stateProvider.state(AccessConstants.UI_STATES.PROJECTS.state,{
            url:'/projects',
            params: {
            },
            views: {
                'content': {
                    templateUrl: 'js/feed-mgr/projects/projects.html',
                    controller:'ProjectsController',
                    controllerAs:'vm'
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController(['feed-mgr/projects/ProjectsController'])
            },
            data:{
                breadcrumbRoot:true,
                displayName:'Projects',
                module:moduleName,
                permissions:AccessConstants.UI_STATES.PROJECTS.permissions
            }
        }).state(AccessConstants.UI_STATES.PROJECT_DETAILS.state,{
            url:'/project-details/{projectId}',
            params: {
                projectId:null
            },
            views: {
                'content': {
                    templateUrl: 'js/feed-mgr/projects/project-details.html',
                    controller: 'ProjectDetailsController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                loadMyCtrl: lazyLoadController(['feed-mgr/projects/project-details'])
            },
            data:{
                breadcrumbRoot:false,
                displayName:'Project Details',
                module:moduleName,
                permissions:AccessConstants.UI_STATES.PROJECT_DETAILS.permissions
            }
        })

    }]);

    function lazyLoadController(path){
        return lazyLoadUtil.lazyLoadController(path,'feed-mgr/projects/module-require');
    }

});
