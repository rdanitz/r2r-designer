'use strict'

angular.module 'app'
  .controller 'PublishCtrl', ($scope, $timeout, $window, _, Rdb, Csv, Rdf, Sml, Transform) ->

    $scope.rdb = Rdb
    $scope.csv = Csv
    $scope.rdf = Rdf
    $scope.sml = Sml
    $scope.transform = Transform

    $scope.publishing = false
    $scope.published = false
    $scope.success = false

    $scope.dumpdb = (table) ->
      mapping =
        source: 'rdb'
        tables: $scope.rdb.selectedTables()
        columns: $scope.rdb.selectedColumns()
        baseUri: $scope.rdf.baseUri
        subjectTemplate: $scope.rdf.subjectTemplate
        classes: $scope.rdf.selectedClasses
        properties: $scope.rdf.selectedProperties
        literals: $scope.rdf.propertyLiteralSelection
        literalTypes: $scope.rdf.propertyLiteralTypes

      w = $window.open ''
      $scope.currentMapping = $scope.sml.toSml mapping, table
      $scope.transform.dumpdb $scope.currentMapping
        .then (url) ->
          w.location = url

    $scope.dumpcsv = (table) ->
      mapping =
        source: 'csv'
        tables: $scope.csv.selectedTables()
        columns: $scope.csv.selectedColumns()
        baseUri: $scope.rdf.baseUri
        subjectTemplate: $scope.rdf.subjectTemplate
        classes: $scope.rdf.selectedClasses
        properties: $scope.rdf.selectedProperties
        literals: $scope.rdf.propertyLiteralSelection
        literalTypes: $scope.rdf.propertyLiteralTypes

      w = $window.open ''
      $scope.currentMapping = $scope.sml.toSml mapping, table
      $scope.transform.dumpcsv $scope.currentMapping
        .then (url) ->
          w.location = url

    $scope.mappingdb = (table) ->
      mapping =
        source: 'rdb'
        tables: $scope.rdb.selectedTables()
        columns: $scope.rdb.selectedColumns()
        baseUri: $scope.rdf.baseUri
        subjectTemplate: $scope.rdf.subjectTemplate
        classes: $scope.rdf.selectedClasses
        properties: $scope.rdf.selectedProperties
        literals: $scope.rdf.propertyLiteralSelection
        literalTypes: $scope.rdf.propertyLiteralTypes
      
      $scope.currentMapping = $scope.sml.toSml mapping, table
      w = $window.open ''
      w.document.open()
      w.document.write '<pre>' + $scope.currentMapping + '</pre>'
      w.document.close()

    $scope.mappingcsv = () ->
      mapping =
        source: 'csv'
        tables: $scope.csv.selectedTables()
        columns: $scope.csv.selectedColumns()
        baseUri: $scope.rdf.baseUri
        subjectTemplate: $scope.rdf.subjectTemplate
        classes: $scope.rdf.selectedClasses
        properties: $scope.rdf.selectedProperties
        literals: $scope.rdf.propertyLiteralSelection
        literalTypes: $scope.rdf.propertyLiteralTypes
      
      $scope.currentMapping = $scope.sml.toSml mapping
      w = $window.open ''
      w.document.open()
      w.document.write '<pre>' + $scope.currentMapping + '</pre>'
      w.document.close()

    $scope.publish = (to) ->
      $scope.publishing = true
      mapping =
        tables: $scope.rdb.selectedTables()
        columns: $scope.rdb.selectedColumns()
        baseUri: $scope.rdf.baseUri
        subjectTemplate: $scope.rdf.subjectTemplate
        classes: $scope.rdf.selectedClasses
        properties: $scope.rdf.selectedProperties
        literals: $scope.rdf.propertyLiteralSelection
        literalTypes: $scope.rdf.propertyLiteralTypes

      $scope.currentMapping = $scope.sml.toSml mapping

      $scope.transform.publish to, $scope.currentMapping
        .success (data) ->
          console.log 'success'
          $scope.publishing = false
          $scope.published = true
          $scope.success = true
        .error (data) ->
          console.log 'error'
          $scope.publishing = false
          $scope.published = true
          $scope.success = false