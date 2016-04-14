// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.plugin.idea.resources.Icons;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.FeedbackAction;
import com.microsoft.alm.plugin.idea.ui.common.FormattedTable;
import com.microsoft.alm.plugin.idea.ui.common.TableModelSelectionConverter;
import com.microsoft.alm.plugin.idea.ui.common.VcsTabStatus;
import com.microsoft.alm.plugin.idea.ui.controls.Hyperlink;
import com.microsoft.alm.plugin.idea.ui.controls.SearchFilter;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.Observable;

public class VcsWorkItemsForm extends Observable {
    private JPanel tabPanel;
    private JScrollPane scrollPanel;
    private FormattedTable workItemsTable;
    private JLabel statusLabel;
    private Hyperlink statusLink;
    private SearchFilter searchFilter;

    //commands
    public static final String CMD_REFRESH = "refresh";
    public static final String CMD_FILTER_CHANGED = "filter";
    public static final String CMD_CREATE_NEW_WORK_ITEM = "createNewWorkItemLink";
    public static final String CMD_STATUS_LINK = "statusLink";
    public static final String CMD_OPEN_SELECTED_WIT_IN_BROWSER = "openSelectedWorkItem";
    public static final String CMD_SEND_FEEDBACK = "sendFeedback";
    public static final String TOOLBAR_LOCATION = "Vcs.WorkItems";

    private boolean initialized = false;
    private Timer timer;

    public VcsWorkItemsForm() {
        ensureInitialized();
    }

    public JComponent getPanel() {
        ensureInitialized();
        return tabPanel;
    }

    private void ensureInitialized() {
        if (!initialized) {
            // Create timer for filtering the list
            timer = new Timer(400, null);
            timer.setInitialDelay(400);
            timer.setActionCommand(CMD_FILTER_CHANGED);
            timer.setRepeats(false);

            workItemsTable = new FormattedTable(WorkItemsTableModel.Column.TITLE.toString());
            workItemsTable.customizeHeader();
            scrollPanel = new JBScrollPane(workItemsTable);
            searchFilter = new SearchFilter();

            //toolbars
            final JPanel toolBarPanel;
            if (ApplicationManager.getApplication() != null) {
                final ActionToolbar witActionsToolbar = createWorkItemActionsToolbar();
                final ActionToolbar feedbackActionsToolbar = createFeedbackActionsToolbar();

                // left panel of the top toolbar
                final FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT, 0, JBUI.scale(3)); // give vertical padding
                final JPanel toolBarPanelLeft = new JPanel(flowLayout);
                toolBarPanelLeft.add(witActionsToolbar.getComponent());
                toolBarPanelLeft.add(searchFilter);

                //entire top toolbar
                toolBarPanel = new JPanel(new BorderLayout());
                toolBarPanel.add(toolBarPanelLeft, BorderLayout.LINE_START);
                toolBarPanel.add(feedbackActionsToolbar.getComponent(), BorderLayout.LINE_END);
            } else {
                //skip setup when called from unit tests
                toolBarPanel = new JPanel();
            }

            //status panel with label and link
            final JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            statusLabel = new JLabel();
            statusLink = new Hyperlink();
            statusLink.setActionCommand(CMD_STATUS_LINK);
            statusPanel.add(statusLabel);
            statusPanel.add(statusLink);

            //tabPanel
            tabPanel = new JPanel(new BorderLayout());
            tabPanel.add(toolBarPanel, BorderLayout.PAGE_START);
            tabPanel.add(scrollPanel, BorderLayout.CENTER);
            tabPanel.add(statusPanel, BorderLayout.PAGE_END);

            this.initialized = true;
        }
    }

    private ActionToolbar createWorkItemActionsToolbar() {
        final AnAction createWorkItemAction = new AnAction(TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_CREATE_WIT),
                TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_CREATE_WIT),
                AllIcons.ToolbarDecorator.Add) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                setChangedAndNotify(CMD_CREATE_NEW_WORK_ITEM);
            }
        };
        createWorkItemAction.registerCustomShortcutSet(CommonShortcuts.getNew(), scrollPanel); //Ctrl+N on windows or Cmd+M on Mac

        final AnAction refreshAction = new AnAction(TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_REFRESH_TOOLTIP),
                TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_REFRESH_TOOLTIP), AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                setChangedAndNotify(CMD_REFRESH);
            }
        };
        refreshAction.registerCustomShortcutSet(CommonShortcuts.getRerun(), scrollPanel); //Ctrl+R on windows or Cmd+R on Mac

        final DefaultActionGroup witActions = new DefaultActionGroup(createWorkItemAction, refreshAction);
        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(TOOLBAR_LOCATION, witActions, false);
        toolbar.setOrientation(SwingConstants.HORIZONTAL);
        toolbar.setTargetComponent(scrollPanel);
        return toolbar;
    }

    private ActionToolbar createFeedbackActionsToolbar() {
        //feedback actions toolbar
        final AnAction sendFeedback = new AnAction(TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_FEEDBACK_DIALOG_TITLE), Icons.Smile) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                final FeedbackAction action = new FeedbackAction(anActionEvent.getProject(),
                        TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_TITLE));
                action.actionPerformed(new ActionEvent(anActionEvent.getInputEvent().getSource(),
                        anActionEvent.getInputEvent().getID(), CMD_SEND_FEEDBACK));
            }
        };

        final DefaultActionGroup feedbackActions = new DefaultActionGroup(sendFeedback);
        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(TOOLBAR_LOCATION, feedbackActions, false);
        toolbar.setOrientation(SwingConstants.HORIZONTAL);
        toolbar.setTargetComponent(scrollPanel);
        return toolbar;
    }

    public void setStatus(final VcsTabStatus status) {
        switch (status) {
            case NOT_TF_GIT_REPO:
                statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_NOT_CONNECTED));
                statusLabel.setIcon(AllIcons.General.Error);
                statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_TITLE));
                statusLink.setVisible(true);
                break;
            case NO_AUTH_INFO:
                statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_NOT_AUTHENTICATED));
                statusLabel.setIcon(AllIcons.General.Error);
                statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_SIGN_IN));
                statusLink.setVisible(true);
                break;
            case LOADING_IN_PROGRESS:
                //Loading in progress
                statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_LOADING));
                statusLabel.setIcon(AllIcons.General.Information);
                statusLink.setText("");
                statusLink.setVisible(false);
                break;
            case LOADING_COMPLETED_ERRORS:
                statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_LOADING_ERRORS));
                statusLabel.setIcon(AllIcons.General.Warning);
                statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_OPEN_IN_BROWSER));
                statusLink.setVisible(true);
                break;
            case LOADING_COMPLETED:
                //loading complete
                statusLabel.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_LAST_REFRESHED_AT, new Date().toString()));
                statusLabel.setIcon(AllIcons.General.Information);
                statusLink.setText(TfPluginBundle.message(TfPluginBundle.KEY_VCS_WIT_OPEN_IN_BROWSER));
                statusLink.setVisible(true);
                break;
            default:
                //we shouldn't get here, nothing to do
                break;
        }
    }

    public void setWorkItemsTable(final WorkItemsTableModel tableModel) {
        workItemsTable.setModel(tableModel);
        workItemsTable.setSelectionModel(tableModel.getSelectionModel());
        workItemsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        searchFilter.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                onFilterChanged();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                onFilterChanged();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                onFilterChanged();
            }

            private void onFilterChanged() {
                if (timer.isRunning()) {
                    timer.restart();
                } else {
                    timer.start();
                }
            }
        });

        // Setup table sorter
        RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel);
        workItemsTable.setRowSorter(sorter);

        // Attach an index converter to fix the indexes if the user sorts the list
        tableModel.setSelectionConverter(new TableModelSelectionConverter() {
            @Override
            public int convertRowIndexToModel(int viewRowIndex) {
                if (viewRowIndex >= 0) {
                    return workItemsTable.convertRowIndexToModel(viewRowIndex);
                }

                return viewRowIndex;
            }
        });
    }

    public void addActionListener(final ActionListener listener) {
        timer.addActionListener(listener);
        statusLink.addActionListener(listener);
        addTableEventListeners(listener);
    }

    private void addTableEventListeners(final ActionListener listener) {
        //mouse listener
        workItemsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                //double click
                if (mouseEvent.getClickCount() == 2) {
                    setChangedAndNotify(CMD_OPEN_SELECTED_WIT_IN_BROWSER);
                } else if (mouseEvent.isPopupTrigger() || ((mouseEvent.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK)) {
                    //right click, show pop up
                    showPopupMenu(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY(), listener);
                }
            }
        });

        //keyboard listener
        workItemsTable.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {

            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    setChangedAndNotify(CMD_OPEN_SELECTED_WIT_IN_BROWSER);
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {

            }
        });
    }

    private void showPopupMenu(final Component component, final int x, final int y, final ActionListener listener) {
        final JBPopupMenu menu = new JBPopupMenu();
        final JBMenuItem openMenuItem = createMenuItem(TfPluginBundle.KEY_VCS_WIT_OPEN_IN_BROWSER, null, VcsWorkItemsForm.CMD_OPEN_SELECTED_WIT_IN_BROWSER, listener);
        menu.add(openMenuItem);
        menu.show(component, x, y);
    }

    private JBMenuItem createMenuItem(final String resourceKey, final Icon icon, final String actionCommand, final ActionListener listener) {
        final String text = TfPluginBundle.message(resourceKey);
        final JBMenuItem menuItem = new JBMenuItem(text, icon);
        menuItem.setActionCommand(actionCommand);
        menuItem.addActionListener(listener);
        return menuItem;
    }

    public void setFilter(final String filterString) {
        searchFilter.setFilterText(filterString);
    }

    public String getFilter() {
        return searchFilter.getFilterText();
    }

    protected void setChangedAndNotify(final String propertyName) {
        super.setChanged();
        super.notifyObservers(propertyName);
    }
}