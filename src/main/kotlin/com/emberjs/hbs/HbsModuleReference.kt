package com.emberjs.hbs

import com.dmarcotte.handlebars.psi.HbMustacheName
import com.emberjs.index.EmberNameIndex
import com.emberjs.lookup.EmberLookupElementBuilder
import com.emberjs.resolver.EmberName
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult.createResults
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.CommonProcessors
import com.intellij.util.FilteringProcessor
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FindSymbolParameters

class HbsModuleReference(element: HbMustacheName, val moduleType: String) :
        PsiPolyVariantReferenceBase<HbMustacheName>(element, TextRange(0, element.textLength), true) {

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {

        val scope = FindSymbolParameters.searchScopeFor(element.project, true)

        val collector = CommonProcessors.CollectProcessor<EmberName>()
        val filter = FilteringProcessor<EmberName>(Condition { it.type == moduleType && it.name == value }, collector)

        val index = FileBasedIndex.getInstance()
        val psiManager = PsiManager.getInstance(element.project)

        // Collect all components from the index
        index.processAllKeys(EmberNameIndex.NAME, filter, scope, null)

        return createResults(collector.results

                // Filter out components that are not related to this project
                .flatMap { index.getContainingFiles(EmberNameIndex.NAME, it, scope) }

                // Convert search results for LookupElements
                .map { psiManager.findFile(it) }
                .filterNotNull())
    }

    override fun getVariants(): Array<out Any?> {
        val scope = FindSymbolParameters.searchScopeFor(element.project, true)

        val collector = CommonProcessors.CollectProcessor<EmberName>()
        val filter = FilteringProcessor<EmberName>(Condition { it.type == moduleType }, collector)

        val index = FileBasedIndex.getInstance()

        // Collect all components from the index
        index.processAllKeys(EmberNameIndex.NAME, filter, scope, null)

        return collector.results

                // Filter out components that are not related to this project
                .filter { index.getContainingFiles(EmberNameIndex.NAME, it, scope).isNotEmpty() }

                // Convert search results for LookupElements
                .map { EmberLookupElementBuilder.create(it) }
                .toTypedArray()
    }
}
