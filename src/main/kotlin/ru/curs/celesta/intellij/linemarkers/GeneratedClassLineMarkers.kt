package ru.curs.celesta.intellij.linemarkers

import ru.curs.celesta.intellij.CelestaConstants

class GeneratedTableLineMarkerProvider : CelestaGeneratedClassLineMarkerProvider() {
    override val parentFqn = CelestaConstants.CURSOR_FQN

    override val objectExtractor: ObjectExtractor = { tableName -> tables[tableName] }
}

class GeneratedMaterializedViewLineMarkers : CelestaGeneratedClassLineMarkerProvider() {
    override val parentFqn = CelestaConstants.MATERIALIZED_VIEW_CURSOR_FQN

    override val objectExtractor: ObjectExtractor = { viewName -> materializedViews[viewName] }
}