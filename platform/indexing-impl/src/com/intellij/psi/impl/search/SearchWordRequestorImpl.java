// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.search;

import com.intellij.model.ModelElement;
import com.intellij.model.search.OccurenceSearchRequestor;
import com.intellij.model.search.SearchWordRequestor;
import com.intellij.model.search.TextOccurenceProcessorProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.search.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static com.intellij.psi.search.UsageSearchContext.*;

final class SearchWordRequestorImpl implements SearchWordRequestor {

  private final @NotNull SearchRequestCollectorImpl myCollector;
  private final @NotNull String myWord;

  private SearchScope mySearchScope;
  private FileType[] myFileTypes;
  private boolean myCaseSensitive = true;
  private Short mySearchContext;
  private ModelElement myTargetHint;

  SearchWordRequestorImpl(@NotNull SearchRequestCollectorImpl collector, @NotNull String word) {
    myCollector = collector;
    myWord = word;
  }

  @NotNull
  private SearchScope getSearchScope() {
    SearchScope scope = mySearchScope != null ? mySearchScope : myCollector.getParameters().getEffectiveSearchScope();
    if (myFileTypes != null && myFileTypes.length > 0) {
      return PsiSearchScopeUtil.restrictScopeTo(scope, myFileTypes);
    }
    else {
      return scope;
    }
  }

  @NotNull
  @Override
  public SearchWordRequestor setSearchScope(@NotNull SearchScope searchScope) {
    mySearchScope = searchScope;
    return this;
  }

  @NotNull
  @Override
  public SearchWordRequestor restrictSearchScopeTo(@NotNull FileType... fileTypes) {
    myFileTypes = fileTypes;
    return this;
  }

  @NotNull
  @Override
  public SearchWordRequestor setCaseSensitive(boolean caseSensitive) {
    myCaseSensitive = caseSensitive;
    return this;
  }

  private short getSearchContext() {
    if (mySearchContext != null) {
      return mySearchContext;
    }
    else {
      int context = IN_CODE | IN_FOREIGN_LANGUAGES | IN_COMMENTS;
      return (short)(context | (myTargetHint instanceof PsiFileSystemItem ? IN_STRINGS : 0));
    }
  }

  @NotNull
  @Override
  public SearchWordRequestor setSearchContext(short searchContext) {
    mySearchContext = searchContext;
    return this;
  }

  @NotNull
  @Override
  public SearchWordRequestor setTargetHint(@NotNull ModelElement target) {
    myTargetHint = target;
    return this;
  }

  @Override
  public void searchRequests(@NotNull OccurenceSearchRequestor occurenceSearchRequestor) {
    searchRequests((element, offsetInElement) -> {
      occurenceSearchRequestor.collectRequests(myCollector, element, offsetInElement);
      return true;
    });
  }

  public void searchRequests(@NotNull TextOccurenceProcessor processor) {
    myCollector.searchWord(createRequests(this), processor);
  }

  @Override
  public void search(@NotNull ModelElement target) {
    setTargetHint(target);
    search(processor -> new SingleTargetOccurenceProcessor(target, processor));
  }

  public void search(@NotNull TextOccurenceProcessorProvider f) {
    myCollector.searchWord(createRequests(this), f);
  }

  @NotNull
  private static Collection<SearchWordRequest> createRequests(@NotNull SearchWordRequestorImpl requestor) {
    SearchScope searchScope = requestor.getSearchScope();
    if (!makesSenseToSearch(searchScope)) {
      return Collections.emptyList();
    }

    String word = requestor.myWord;
    ModelElement targetHint = requestor.myTargetHint;
    short searchContext = requestor.getSearchContext();
    boolean caseSensitive = requestor.myCaseSensitive;

    if (targetHint != null && searchScope instanceof GlobalSearchScope && (searchContext & IN_CODE) != 0) {
      SearchScope restrictedCodeUsageSearchScope = null;
      // ReadAction.compute(() -> ScopeOptimizer.calculateOverallRestrictedUseScope(CODE_USAGE_SCOPE_OPTIMIZER_EP_NAME.getExtensions(), searchTarget));
      //noinspection ConstantConditions
      if (restrictedCodeUsageSearchScope != null) {
        short nonCodeSearchContext = searchContext == ANY ? IN_COMMENTS | IN_STRINGS | IN_FOREIGN_LANGUAGES | IN_PLAIN_TEXT
                                                          : (short)(searchContext ^ IN_CODE);
        SearchScope codeScope = searchScope.intersectWith(restrictedCodeUsageSearchScope);
        SearchWordRequest codeRequest = new SearchWordRequest(
          word, codeScope, caseSensitive, IN_CODE, null
        );
        SearchWordRequest nonCodeRequest = new SearchWordRequest(
          word, searchScope, caseSensitive, nonCodeSearchContext, null
        );

        return Arrays.asList(codeRequest, nonCodeRequest);
      }
    }
    return Collections.singleton(new SearchWordRequest(word, searchScope, caseSensitive, searchContext, null));
  }

  private static boolean makesSenseToSearch(@NotNull SearchScope searchScope) {
    if (searchScope instanceof LocalSearchScope && ((LocalSearchScope)searchScope).getScope().length == 0) {
      return false;
    }
    else {
      return searchScope != GlobalSearchScope.EMPTY_SCOPE;
    }
  }
}
