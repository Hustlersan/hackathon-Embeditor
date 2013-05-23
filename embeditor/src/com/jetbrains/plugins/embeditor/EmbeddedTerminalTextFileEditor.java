package com.jetbrains.plugins.embeditor;

import com.google.common.collect.Maps;
import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileEditor.impl.FileEditorProviderManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.UIUtil;
import com.jediterm.emulator.TtyConnector;
import com.jediterm.pty.PtyProcess;
import com.jediterm.pty.PtyProcessTtyConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.JBTerminal;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author traff
 */
public class EmbeddedTerminalTextFileEditor extends UserDataHolderBase implements FileEditor, TextEditor {
  private final Project myProject;
  private final VirtualFile myFile;

  private TextEditor myEditor;

  private JComponent myEditorPanel;

  public EmbeddedTerminalTextFileEditor(Project project, VirtualFile vFile) {
    myProject = project;

    myFile = vFile;

    final JBTerminal terminal = new JBTerminal();

    Map<String, String> env = Maps.newHashMap(System.getenv());
    env.put("TERM", "vt100");

    final PtyProcess process = new PtyProcess("/usr/bin/vim", new String[]{"/usr/bin/vim", vFile.getPath()}, env);

    terminal.setTtyConnector(createTtyConnector(process));

    myEditor = createEditor(project, vFile);

    myEditorPanel = terminal.getTermPanel();

    terminal.start();

    new Thread(new Runnable() {

      @Override
      public void run() {
        while (!process.isFinished()) {
          try {
            process.waitFor();
          }
          catch (InterruptedException e) {

          }
        }

        if (process.isFinished()) {
          UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
              close();
            }
          });
        }
      }
    }).start();
  }


  protected TtyConnector createTtyConnector(PtyProcess process) {
    return new PtyProcessTtyConnector(process, Charset.defaultCharset());
  }

  public void close() {
    FileEditorManagerImpl.getInstance(myProject).closeFile(myFile);
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myEditorPanel;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myEditorPanel;
  }

  @NotNull
  @Override
  public String getName() {
    return "";
  }

  @NotNull
  @Override
  public FileEditorState getState(@NotNull FileEditorStateLevel level) {
    return new MyEditorState(-1, ArrayUtil.EMPTY_STRING_ARRAY);
  }

  @Override
  public void setState(@NotNull FileEditorState state) {
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void selectNotify() {
  }

  @Override
  public void deselectNotify() {
  }

  @Override
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
  }

  @Override
  public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
  }

  @Override
  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    return null;
  }

  @Override
  public FileEditorLocation getCurrentLocation() {
    return null;
  }

  @Override
  public StructureViewBuilder getStructureViewBuilder() {
    return null;
  }

  @Override
  public void dispose() {
    Disposer.dispose(myEditor);
  }

  @NotNull
  @Override
  public Editor getEditor() {
    return myEditor.getEditor();
  }

  @Override
  public boolean canNavigateTo(@NotNull Navigatable navigatable) {
    return true;
  }

  @Override
  public void navigateTo(@NotNull Navigatable navigatable) {
  }

  @Nullable
  private static TextEditor createEditor(@NotNull Project project, @NotNull VirtualFile vFile) {
    FileEditorProvider provider = getProvider(project, vFile);

    if (provider != null) {
      FileEditor editor = provider.createEditor(project, vFile);
      if (editor instanceof TextEditor) {
        return (TextEditor)editor;
      }
    }
    return null;
  }

  @Nullable
  private static FileEditorProvider getProvider(Project project, VirtualFile vFile) {
    FileEditorProvider[] providers = FileEditorProviderManagerImpl.getInstance().getProviders(project, vFile);
    for (FileEditorProvider provider : providers) {
      if (!(provider instanceof EmbeddedTerminalEditorProvider)) {
        return provider;
      }
    }
    return null;
  }
}