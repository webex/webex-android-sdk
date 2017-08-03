package com.cisco.spark.android.model;

import android.text.TextUtils;

import com.cisco.spark.android.R;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.ui.conversation.SearchResultsSection.SectionType;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.wdm.DeviceRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.inject.Singleton;

@Singleton
public class SearchStringWithModifiers {
    private static final HashMap<SearchModifiers, List<SectionType>> searchResultSectionsForEachModifier = new HashMap<SearchStringWithModifiers.SearchModifiers, List<SectionType>>() {
        {
            put(SearchStringWithModifiers.SearchModifiers.FROM, Arrays.asList(SectionType.SECTION_TYPE_MESSAGES, SectionType.SECTION_TYPE_CONTENT));
            put(SearchStringWithModifiers.SearchModifiers.ROOMSWITH, Arrays.asList(SectionType.SECTION_TYPE_CONVERSATIONS));
            put(SearchStringWithModifiers.SearchModifiers.SHAREDIN, Arrays.asList(SectionType.SECTION_TYPE_MESSAGES, SectionType.SECTION_TYPE_CONTENT));
        }
    };
    private String originalSearchString;
    private String searchStringWithNoModifier = "";
    private String fromModifierUUID = "";
    private String fromModifierSearchString = "";
    private String inModifierUUID = "";
    private String inModifierSearchString = "";
    private List<String> withModifierUUID = new ArrayList<>();
    private List<String> withModifierSearchString = new ArrayList<>();
    private String lastSearchString = "";
    private SearchModifiers updatingModifierType = null;
    private DeviceRegistration deviceRegistration;

    public SearchStringWithModifiers(DeviceRegistration deviceRegistration) {
        this.deviceRegistration = deviceRegistration;
    }

    public String getOriginalSearchString() {
        return originalSearchString;
    }

    public SearchModifiers getUpdatingModifierType() {
        return updatingModifierType;
    }

    //TODO: Support multiple modifiers
    public boolean updateModifierType(String s) {
        //Parse searchString
        originalSearchString = s;
        boolean updatingModifier = containsModifier(s.toLowerCase());
        if (!updatingModifier) {
            lastSearchString = s;
            searchStringWithNoModifier = s;
        }
        return updatingModifier;
    }

    private boolean containsModifier(String modifierString) {
        if (!TextUtils.isEmpty(modifierString)) {
            boolean foundModifier = false;
            for (SearchModifiers searchModifier : SearchModifiers.values()) {
                modifierString = Strings.removeSpacesAroundSearchModifierDelimiter(modifierString);
                int index = modifierString.indexOf(searchModifier.getModifierString());
                if (index >= 0) {
                    foundModifier = true;
                    if (setUpdatingModifierType(searchModifier)) {
                        updatingModifierType = searchModifier;
                    } else {
                        //Check for deletion
                        if (updatingModifierType != null) {
                            triggerClearRecipient(modifierString.substring(index + updatingModifierType.getModifierString().length()));
                        }
                    }
                }
            }
            if (foundModifier) {
                extractSearchStringWithNoModifiers(lastSearchString, modifierString);
            }
            return foundModifier;
        }
        updatingModifierType = null;
        return false;
    }

    public boolean isUpdatingModifier() {
        return (updatingModifierType != null && setUpdatingModifierType(updatingModifierType));
    }

    private boolean setUpdatingModifierType(SearchModifiers searchModifier) {
        switch (searchModifier) {
            case FROM:
                return TextUtils.isEmpty(fromModifierUUID);
            case SHAREDIN:
                return TextUtils.isEmpty(inModifierUUID);
            case ROOMSWITH:
                return withModifierUUID.isEmpty();
        }
        return false;
    }

    private void extractSearchStringWithNoModifiers(String lastSearchString, String currentSearchString) {
        currentSearchString = currentSearchString.toLowerCase();
        //remove all occurrences of modifier identifiers and their corresponding ModifierSearchStrings
        for (SearchModifiers searchModifier : SearchModifiers.values()) {
            int index = currentSearchString.indexOf(searchModifier.getModifierString());
            if (index >= 0 && !TextUtils.isEmpty(currentSearchString)) {
                switch (searchModifier) {
                    case FROM:
                        if (!TextUtils.isEmpty(fromModifierSearchString) && currentSearchString.indexOf(fromModifierSearchString.toLowerCase()) == index + searchModifier.getModifierString().length()) {
                            currentSearchString = currentSearchString.replace(searchModifier.getModifierString(), "").toLowerCase();
                            currentSearchString = currentSearchString.replace(fromModifierSearchString.toLowerCase(), "");
                        }
                        break;
                    case SHAREDIN:
                        if (!TextUtils.isEmpty(inModifierSearchString) && currentSearchString.indexOf(inModifierSearchString.toLowerCase()) == index + searchModifier.getModifierString().length()) {
                            currentSearchString = currentSearchString.replace(searchModifier.getModifierString(), "").toLowerCase();
                            currentSearchString = currentSearchString.replace(inModifierSearchString.toLowerCase(), "");
                        }
                        break;
                    case ROOMSWITH:
                        if (!withModifierSearchString.isEmpty()) {
                            for (String string : withModifierSearchString) {
                                if (currentSearchString.indexOf(string) == index + searchModifier.getModifierString().length()) {
                                    currentSearchString = currentSearchString.replace(searchModifier.getModifierString(), "").toLowerCase();
                                    currentSearchString = currentSearchString.replace(string.toLowerCase(), "");
                                    currentSearchString = currentSearchString.replaceFirst(",", "");
                                }
                            }

                        }
                        break;
                }
                //Extract currently updating searchString
                if (searchModifier.equals(updatingModifierType) && isUpdatingModifier()) {
                    lastSearchString = currentSearchString.substring(index + updatingModifierType.getModifierString().length());
                    currentSearchString = currentSearchString.replace(updatingModifierType.getModifierString(), "");
                }
            }
        }
        searchStringWithNoModifier = currentSearchString.replace(lastSearchString, "").trim();
        this.lastSearchString = lastSearchString.trim();

    }

    public void updateRecipents(String uuid, String conversationId, String searchString) {
        switch (updatingModifierType) {
            case FROM:
                fromModifierUUID = uuid;
                fromModifierSearchString = searchString;
                break;
            case SHAREDIN:
                inModifierUUID = conversationId;
                inModifierSearchString = searchString;
                break;
            case ROOMSWITH:
                withModifierUUID.add(uuid);
                withModifierSearchString.add(searchString);
                break;

        }
    }

    public boolean usesModifiers() {
        return !fromModifierUUID.isEmpty() || isUUIDPresent() || !withModifierUUID.isEmpty();
    }

    public List<SearchModifiers> getSearchModifiersUsedList() {
        ArrayList<SearchModifiers> searchModifiersUsed = new ArrayList<>();
        if (!fromModifierUUID.isEmpty()) {
            searchModifiersUsed.add(SearchModifiers.FROM);
        }
        if (isUUIDPresent()) {
            searchModifiersUsed.add(SearchModifiers.SHAREDIN);
        }
        if (!withModifierUUID.isEmpty()) {
            searchModifiersUsed.add(SearchModifiers.ROOMSWITH);
        }
        return searchModifiersUsed;
    }

    public boolean queryDirectory() {
        return updatingModifierType == null ||
                updatingModifierType != null && updatingModifierType.equals(SearchModifiers.FROM) && fromModifierUUID.isEmpty() || updatingModifierType.equals(SearchModifiers.ROOMSWITH) && withModifierUUID.size() < 5;
    }

    public List<SectionType> getSearchResultSectionTypesForCurrentModifier() {
        if (usesModifiers()) {
            if (!withModifierUUID.isEmpty()) {
                return searchResultSectionsForEachModifier.get(SearchModifiers.ROOMSWITH);
            } else if (!fromModifierUUID.isEmpty()) {
                return searchResultSectionsForEachModifier.get(SearchModifiers.FROM);
            } else if (!inModifierUUID.isEmpty()) {
                return searchResultSectionsForEachModifier.get(SearchModifiers.SHAREDIN);
            }
        }
        return null;
    }

    public String getSearchStringWithNoModifier() {
        return searchStringWithNoModifier;
    }

    public String getLastSearchString() {
        return lastSearchString;
    }

    public String getFromModifierUUID() {
        return fromModifierUUID;
    }

    public void setFromModifierUUID(String fromModifierUUID) {
        this.fromModifierUUID = fromModifierUUID;
    }

    public String getInModifierUUID() {
        return inModifierUUID;
    }

    public void setInModifierUUID(String inModifierUUID) {
        this.inModifierUUID = inModifierUUID;
    }

    public List<String> getWithModifierUUID() {
        return withModifierUUID;
    }

    public String getSelectionCriteriaForConversationSearch() {
        StringBuffer selection = new StringBuffer();
        if (!withModifierUUID.isEmpty()) {
            selection.append(SearchModifiers.ROOMSWITH.getLocalConversationSearchSelection() + Strings.join(",", withModifierUUID) + ")");
        }
        String searchString = lastSearchString.trim();
        if (!TextUtils.isEmpty(searchString) && updatingModifierType != null) {
            if (updatingModifierType.equals(SearchModifiers.SHAREDIN) && inModifierUUID.isEmpty()) {
                selection.append(ConversationContract.ConversationSearchEntry.CONVERSATION_NAME + ":" + searchString + "*");
            }
            if (updatingModifierType.equals(SearchModifiers.FROM) && fromModifierUUID.isEmpty()) {
                selection.append(ConversationContract.ConversationSearchEntry.CONVERSATION_NAME + ":" + searchString + "* OR " + ConversationContract.ConversationSearchEntry.PARTICIPANT_NAMES + ":" + searchString);
            }
        }
        return selection.toString();
    }

    public String getSelectionCriteriaForMessageSearchWithModifiers() {
        StringBuffer args = new StringBuffer();
        if (isUUIDPresent()) {
            args.append(SearchModifiers.SHAREDIN.getLocalMessageSearchSelection());
        }
        return args.toString();
    }

    public List<String> getSelectionArgsForMessageSearchWithModifiers() {
        List<String> selectionArgsForMessageSearch = new ArrayList<>();
        StringBuffer args = new StringBuffer();
        if (isUUIDPresent()) {
            selectionArgsForMessageSearch.add(inModifierUUID);
        }

        if (!fromModifierUUID.isEmpty()) {
            args.append(SearchModifiers.FROM.getLocalMessageSearchSelection() + fromModifierSearchString);
        }

        if (!TextUtils.isEmpty(searchStringWithNoModifier.trim())) {
            if (args.length() > 0) {
                args.append(" ");
            }
            args.append(ConversationContract.MessageSearchEntry.MESSAGE_TEXT + ":" + searchStringWithNoModifier + "*");
        }
        if (args.length() > 0) {
            selectionArgsForMessageSearch.add(args.toString());
        }
        return selectionArgsForMessageSearch;
    }

    public String getSelectionCriteriaForContentSearchWithModifiers() {
        StringBuffer args = new StringBuffer();
        if (isUUIDPresent()) {
            args.append(SearchModifiers.SHAREDIN.getLocalContentSearchSelection());
        }

        return args.toString();
    }

    public List<String> getSelectionArgsForContentSearchWithModifiers() {
        List<String> selectionArgsForContentSearch = new ArrayList<>();
        StringBuffer args = new StringBuffer();
        if (isUUIDPresent()) {
            selectionArgsForContentSearch.add(inModifierUUID);
        }

        if (!fromModifierUUID.isEmpty()) {
            args.append(SearchModifiers.FROM.getLocalContentSearchSelection() + fromModifierSearchString);
        }

        if (!TextUtils.isEmpty(searchStringWithNoModifier.trim())) {
            if (args.length() > 0) {
                args.append(" ");
            }
            args.append(ConversationContract.ContentSearchEntry.CONTENT_TITLE + ":" + searchStringWithNoModifier + "* OR " +
                    ConversationContract.ContentSearchEntry.CONTENT_TYPE + ":" + searchStringWithNoModifier + "*");
        }
        if (args.length() > 0) {
            selectionArgsForContentSearch.add(args.toString());
        }
        return selectionArgsForContentSearch;
    }

    public void triggerClearRecipient(String lastSearchString) {
        if (updatingModifierType != null) {
            String updatingRecipientName = "";
            switch (updatingModifierType) {
                case FROM:
                    updatingRecipientName = fromModifierSearchString;
                    break;
                case ROOMSWITH:
                    if (!withModifierSearchString.isEmpty()) {
                        updatingRecipientName = withModifierSearchString.get(withModifierSearchString.size() - 1);
                    }
                    break;
                case SHAREDIN:
                    updatingRecipientName = inModifierSearchString;
                    break;
            }

            if (!TextUtils.isEmpty(updatingRecipientName)) {
                updatingRecipientName = updatingRecipientName.substring(0, updatingRecipientName.length() - 1);
                if (!TextUtils.isEmpty(lastSearchString) && lastSearchString.equalsIgnoreCase(updatingRecipientName)) {
                    switch (updatingModifierType) {
                        case FROM:
                            fromModifierSearchString = "";
                            fromModifierUUID = "";
                            break;
                        case ROOMSWITH:
                            if (!withModifierSearchString.isEmpty()) {
                                withModifierSearchString.remove(withModifierSearchString.size() - 1);
                            }
                            if (!withModifierUUID.isEmpty()) {
                                withModifierUUID.remove(withModifierUUID.size() - 1);
                            }
                            break;
                        case SHAREDIN:
                            inModifierSearchString = "";
                            inModifierUUID = "";
                            break;
                    }
                }
            }
        }
    }

    public void clearAllRecipients() {
        fromModifierUUID = "";
        inModifierUUID = "";
        withModifierUUID.clear();
        fromModifierSearchString = "";
        inModifierSearchString = "";
        withModifierSearchString.clear();
    }

    public void clear() {
        clearAllRecipients();
        searchStringWithNoModifier = "";
        updatingModifierType = null;
    }

    public boolean includeSelfInPeopleResults() {
        return (isUpdatingModifier() && getUpdatingModifierType().equals(SearchStringWithModifiers.SearchModifiers.FROM));
    }

    private boolean isUUIDPresent() {
        return !inModifierUUID.isEmpty();
    }

    public void setSearchString(String query) {
        if (!TextUtils.isEmpty(query) && updatingModifierType != null) {
            originalSearchString = Strings.removeSpacesAroundSearchModifierDelimiter(originalSearchString);
            int index = originalSearchString.indexOf(updatingModifierType.getModifierString());
            if (index >= 0) {
                originalSearchString = originalSearchString.substring(0, index + updatingModifierType.getModifierString().length());
                originalSearchString += query;
            }
        }
    }


    public enum SearchModifiers {
        FROM("from:", SectionType.SECTION_TYPE_PEOPLE, "", "MESSAGE_POSTED_BY:", "CONTENT_SHARED_BY:", R.string.search_hint_for_modifier),
        ROOMSWITH("with:", SectionType.SECTION_TYPE_PEOPLE, " AND PARTICIPANT_NAMES IN (", "", "", R.string.search_hint_with_modifier),
        SHAREDIN("in:", SectionType.SECTION_TYPE_CONVERSATIONS, "", "suggest_intent_data=?", "suggest_intent_data=?", R.string.search_hint_in_modifier);

        private final String modifierString;
        private final SectionType sectionType;
        private final String localConversationSearchSelection;
        private final String localMessageSearchSelection;
        private final String localContentSearchSelection;
        private final int hint;

        SearchModifiers(String modifierString, SectionType sectionType, String conversationSelection, String messageSelection, String contentSelection, int hint) {
            this.modifierString = modifierString;
            this.sectionType = sectionType;
            this.localConversationSearchSelection = conversationSelection;
            this.localMessageSearchSelection = messageSelection;
            this.localContentSearchSelection = contentSelection;
            this.hint = hint;
        }

        public String getModifierString() {
            return modifierString;
        }

        public int getHint() {
            return hint;
        }

        public SectionType getSectionType() {
            return sectionType;
        }

        public String getLocalConversationSearchSelection() {
            return localConversationSearchSelection;
        }

        public String getLocalMessageSearchSelection() {
            return localMessageSearchSelection;
        }

        public String getLocalContentSearchSelection() {
            return localContentSearchSelection;
        }

    }
}

