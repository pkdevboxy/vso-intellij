// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.intellij.openapi.ui.JBMenuItem;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.ui.common.tabs.TabForm;
import com.microsoft.alm.plugin.idea.ui.controls.WorkItemQueryDropDown;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.WorkItemLookupOperation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VcsWorkItemsFormTest extends IdeaAbstractTest {
    VcsWorkItemsForm underTest;

    @Mock
    private WorkItemQueryDropDown mockWorkItemQueryDropDown;

    @Before
    public void setUp() throws Exception {
        underTest = spy(new VcsWorkItemsForm(mockWorkItemQueryDropDown));
    }

    @Test
    public void testCreateCustomView() {
        underTest.createCustomView();
        assertNotNull(underTest.getWorkItemsTable());
    }

    @Test
    public void testGetMenuItems() {
        List<JBMenuItem> menuItemList = underTest.getMenuItems(null);
        assertEquals(2, menuItemList.size());
        assertEquals(TabForm.CMD_OPEN_SELECTED_ITEM_IN_BROWSER, menuItemList.get(0).getActionCommand());
        assertEquals(VcsWorkItemsForm.CMD_CREATE_BRANCH, menuItemList.get(1).getActionCommand());
    }

    @Test
    public void testGetOperationInputs() {
        when(mockWorkItemQueryDropDown.getSelectedResults()).thenReturn("wiql");
        Operation.Inputs inputs = underTest.getOperationInputs();
        verify(mockWorkItemQueryDropDown).getSelectedResults();
        assertTrue(inputs instanceof WorkItemLookupOperation.WitInputs);
    }

    @Test
    public void testRefresh() {
        underTest.refresh();
        verify(mockWorkItemQueryDropDown).refreshDropDown();
    }
}
