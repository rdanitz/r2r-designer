'use strict'

angular.module 'r2rDesignerApp'
  .controller 'CsvReviseCtrl', ($scope, _, Csv, Rdf) ->

    $scope.csv = Csv
    $scope.rdf = Rdf

    $scope.table = ''
    $scope.columns = []

    $scope.$watch 'csv.csvFile()', (val) ->
      if val?
        $scope.table = val.name

    $scope.$watch 'csv.selectedColumns()[table]', (val) ->
      if val?
        $scope.columns = $scope.csv.selectedColumns()[$scope.table]

    $scope.selectedColumns = []
    $scope.cursorpos = 0
    $scope.hasColumns = () -> !_.isEmpty $scope.columns
    $scope.isSelected = (column) -> _.contains $scope.selectedColumns, column

    $scope.insert = (column) ->
      if ($scope.isSelected column)
        oldVal = $scope.rdf.subjectTemplate
        $scope.rdf.subjectTemplate = oldVal.replace '{' + column + '}', ''
        $scope.selectedColumns = _.without $scope.selectedColumns, column
      else
        oldVal = $scope.rdf.subjectTemplate
        $scope.rdf.subjectTemplate = (oldVal.slice 0, $scope.cursorpos) + '{' + column + '}' + (oldVal.slice $scope.cursorpos, oldVal.length)
        $scope.selectedColumns.push column

