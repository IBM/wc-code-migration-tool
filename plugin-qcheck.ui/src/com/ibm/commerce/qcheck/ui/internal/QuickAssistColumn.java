package com.ibm.commerce.qcheck.ui.internal;

/*
 *-----------------------------------------------------------------
 * Copyright 2018 Trent Hoeppner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *-----------------------------------------------------------------
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.source.AbstractRulerColumn;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension2;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.HackyAccessTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.texteditor.rulers.IContributedRulerColumn;
import org.eclipse.ui.texteditor.rulers.RulerColumnDescriptor;

import com.ibm.commerce.qcheck.core.Debug;
import com.ibm.commerce.qcheck.core.ProblemAction;
import com.ibm.commerce.qcheck.ui.Activator;

/**
 * This class provides support for quick fixes in non-Java text editors. This is
 * added to the UI through the
 * <code>org.eclipse.ui.workbench.texteditor.rulerColumns</code>.
 * 
 * @author Trent Hoeppner
 */
@SuppressWarnings("restriction")
public class QuickAssistColumn extends AbstractRulerColumn implements IContributedRulerColumn {

	/**
	 * The color white, used to paint the background when a quick assist icon is
	 * not needed.
	 */
	private static final Color WHITE = new Color(null, 255, 255, 255);

	/**
	 * The resource bundle key used for the bundle when invoking quick assist.
	 */
	private static final String EDITOR_QUICK_ASSIST_KEY = "Editor.QuickAssist.";

	/**
	 * The bundle that is used when invoking quick assist.
	 */
	private static final ResourceBundle BUNDLE = new ListResourceBundle() {

		@Override
		protected Object[][] getContents() {
			return new Object[][] { { EDITOR_QUICK_ASSIST_KEY, "Some QuickAssist value" } };
		}
	};

	/**
	 * The descriptor for this column. This value is maintained in case Eclipse
	 * APIs need to access it.
	 */
	private RulerColumnDescriptor descriptor;

	/**
	 * The editor to which this column is currently attached. Note that this
	 * value will change when switching to a different editor, and may be null.
	 */
	private ITextEditor editor;

	/**
	 * True indicates that quick assist has been added to the editor, false
	 * indicates that it has not yet been added.
	 */
	private boolean editorConfiguredWithQuickAssist = false;

	/**
	 * The 0-based line numbers that have quick assist available.
	 */
	private Set<Integer> linesWithAssist;

	/**
	 * The icon to show in the column for lines that have quick assist
	 * available.
	 */
	private Image image;

	/**
	 * A mapping from text ranges to actions that can be taken for the ranges.
	 */
	private Map<Range, List<ProblemAction>> rangeToActionsMap;

	/**
	 * Constructor for this.
	 */
	public QuickAssistColumn() {
		try {
			InputStream input = FileLocator.openStream(Activator.getDefault().getBundle(),
					new Path("icons" + File.separator + "quickfix.png"), false);
			image = new Image(Display.getCurrent(), input);
		} catch (IOException e) {
			Debug.FRAMEWORK.log(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Control createControl(CompositeRuler parentRuler, Composite parentControl) {
		if (isJavaEditor()) {
			return super.createControl(parentRuler, parentControl);
		}
		Control control = super.createControl(parentRuler, parentControl);
		control.addMouseListener(new QuickAssistMouseListener());
		return control;
	}

	/**
	 * Returns whether the editor in this is a Java editor.
	 *
	 * @return True if the editor is a Java editor, false otherwise.
	 */
	private boolean isJavaEditor() {
		return editor instanceof CompilationUnitEditor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void paintLine(GC gc, int modelLine, int widgetLine, int linePixel, int lineHeight) {

		if (editor == null || linesWithAssist == null) {
			return;
		}

		if (linesWithAssist.contains(modelLine)) {
			gc.drawImage(image, 0, linePixel);
		} else {
			gc.setForeground(WHITE);
			gc.fillRectangle(0, linePixel, image.getImageData().width, linePixel + lineHeight);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void columnCreated() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void columnRemoved() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RulerColumnDescriptor getDescriptor() {
		return descriptor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ITextEditor getEditor() {
		return editor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDescriptor(RulerColumnDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setEditor(ITextEditor arg0) {
		this.editor = arg0;

		if (!isJavaEditor()) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			workspace.addResourceChangeListener(new QuickAssistResourceChangeListener(),
					IResourceChangeEvent.POST_BUILD);

			refreshLinesWithAssist();
		}

		editorConfiguredWithQuickAssist = false;
	}

	/**
	 * Refreshes the quick assist after an editor change or a change in the
	 * resource the editor is acting upon. The {@link #linesWithAssist} is
	 * modified as is the {@link #rangeToActionsMap}.
	 */
	private void refreshLinesWithAssist() {
		linesWithAssist = new HashSet<Integer>();
		rangeToActionsMap = new HashMap<Range, List<ProblemAction>>();

		if (editor == null) {
			return;
		}

		IResource resource = ResourceUtil.getResource(editor.getEditorInput());
		if (resource == null) {
			return;
		}

		try {

			IMarker[] markers = resource.findMarkers(UIValidationRunner.MARKER_TYPE, true, IResource.DEPTH_ZERO);
			UIActionFactory factory = new UIActionFactory();
			for (IMarker marker : markers) {
				int numSuggestions = marker.getAttribute(UIValidationRunner.MARKER_ATT_NUM_SUGGESTIONS, 0);
				if (numSuggestions > 0) {
					int lineNumber = marker.getAttribute(IMarker.LINE_NUMBER, -1) - 1;
					linesWithAssist.add(lineNumber);

					int startPos = marker.getAttribute(IMarker.CHAR_START, -1);
					int length = marker.getAttribute(IMarker.CHAR_END, -1) - startPos;
					Range range = new Range(startPos, length);
					List<ProblemAction> actions = rangeToActionsMap.get(range);
					if (actions == null) {
						actions = new ArrayList<ProblemAction>();
						rangeToActionsMap.put(range, actions);
					}

					for (int i = 0; i < numSuggestions; i++) {
						String xml = marker.getAttribute(UIValidationRunner.MARKER_ATT_SUGGESTION_PREFIX + i, null);
						try {
							ProblemAction action = factory.unmarshal(xml);
							actions.add(action);
						} catch (IOException e) {
							Debug.FRAMEWORK.log(e);
						}
					}

					break;
				}
			}

		} catch (CoreException e) {
			Debug.FRAMEWORK.log(e);
		}
	}

	/**
	 * This class represents a range of text in a document.
	 */
	private static class Range {

		private int startPos;

		private int length;

		/**
		 * Constructor for this.
		 *
		 * @param startPos
		 *            The 0-based index into the characters in the document.
		 *            This value must be &gt;= 0.
		 * @param length
		 *            The number of characters in the range. This value must be
		 *            &gt;= 0.
		 */
		public Range(int startPos, int length) {
			this.startPos = startPos;
			this.length = length;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + length;
			result = prime * result + startPos;
			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}

			Range other = (Range) obj;
			if (length != other.length) {
				return false;
			}
			if (startPos != other.startPos) {
				return false;
			}

			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return "R[startPos=" + startPos + ",length=" + length + "]";
		}

		/**
		 * Returns whether this range contains the given offset.
		 *
		 * @param offset
		 *            The offset to check. This value must be &gt;= 0.
		 *
		 * @return True if the offset is in this, false otherwise.
		 */
		public boolean contains(int offset) {
			return startPos <= offset && offset < startPos + length;
		}

	}

	/**
	 * This class is used to invoke quick assist when the user clicks on the
	 * icon.
	 */
	private class QuickAssistMouseListener implements MouseListener {

		/**
		 * Constructor for this.
		 */
		public QuickAssistMouseListener() {
			// do nothing
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void mouseDoubleClick(MouseEvent arg0) {
			// do nothing
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void mouseDown(MouseEvent arg0) {
			if (arg0.button != 1) {
				return;
			}

			if (!editorConfiguredWithQuickAssist && editor instanceof AbstractTextEditor) {
				AbstractTextEditor textEditor = (AbstractTextEditor) editor;

				HackyAccessTextEditor editorForAccess = new HackyAccessTextEditor(textEditor);

				ISourceViewer viewer = editorForAccess.getEditorSourceViewer();
				ISourceViewerExtension2 viewerAsExtension2 = (ISourceViewerExtension2) viewer;
				viewerAsExtension2.unconfigure();
				viewer.configure(new OverridingSourceViewerConfiguration());
				editorConfiguredWithQuickAssist = true;
			}

			TextOperationAction action = new TextOperationAction(BUNDLE, EDITOR_QUICK_ASSIST_KEY, editor,
					ISourceViewer.QUICK_ASSIST);
			action.run();

			Object source = arg0.getSource();
			Debug.FRAMEWORK.log("source is " + source);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void mouseUp(MouseEvent arg0) {
			// do nothing
		}

	}

	/**
	 * This class is used to modify the default TextSourceViewerConfiguration to
	 * return a IQuickAssistAssistant to be used by the editor.
	 */
	private class OverridingSourceViewerConfiguration extends TextSourceViewerConfiguration {

		/**
		 * Constructor for this.
		 */
		public OverridingSourceViewerConfiguration() {
			// do nothing
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
			IQuickAssistAssistant assistant = new QuickAssistAssistant();
			assistant.setQuickAssistProcessor(new ActionExecutionProcessor());
			return assistant;
		}

	}

	/**
	 * This class is used to invoke actions when the user selects an action in
	 * the quick assist dialog.
	 */
	private class ActionExecutionProcessor implements IQuickAssistProcessor {

		/**
		 * Constructor for this.
		 */
		public ActionExecutionProcessor() {
			// do nothing
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean canAssist(IQuickAssistInvocationContext paramIQuickAssistInvocationContext) {
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean canFix(Annotation paramAnnotation) {
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext context) {
			int offset = context.getOffset();

			List<ProblemAction> actions = null;
			for (Range possibleRange : rangeToActionsMap.keySet()) {
				if (possibleRange.contains(offset)) {
					actions = rangeToActionsMap.get(possibleRange);
					break;
				}
			}

			if (actions == null) {
				Debug.FRAMEWORK.log("No actions available for offset: " + offset);
				return null;
			}

			ICompletionProposal[] proposals = new ICompletionProposal[actions.size()];
			for (int i = 0; i < actions.size(); i++) {
				ProblemAction action = actions.get(i);
				proposals[i] = new ProblemActionProposal(action);
			}

			return proposals;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getErrorMessage() {
			return "An error was generated from ActionExecutionProcessor";
		}

	}

	/**
	 * This class wraps a {@link ProblemAction} to allow it to be invoked by
	 * quick assist.
	 */
	private class ProblemActionProposal implements ICompletionProposal {

		/**
		 * The action to wrap.
		 */
		private ProblemAction action;

		/**
		 * Constructor for this.
		 *
		 * @param action
		 *            The action to wrap. This value cannot be null.
		 */
		public ProblemActionProposal(ProblemAction action) {
			this.action = action;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void apply(IDocument paramIDocument) {
			try {
				action.execute();
			} catch (IOException e) {
				Debug.FRAMEWORK.log(e);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getAdditionalProposalInfo() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IContextInformation getContextInformation() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getDisplayString() {
			return action.getDescription();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Image getImage() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Point getSelection(IDocument paramIDocument) {
			return null;
		}

	}

	/**
	 * This class listens to changes in resources so it can detect when quick
	 * assist data needs to be refreshed.
	 */
	private class QuickAssistResourceChangeListener implements IResourceChangeListener {

		/**
		 * Constructor for this.
		 */
		public QuickAssistResourceChangeListener() {
			// do nothing
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void resourceChanged(IResourceChangeEvent arg0) {
			IResource resource = ResourceUtil.getResource(editor.getEditorInput());
			if (resource == null) {
				return;
			}

			IResourceDelta delta = arg0.getDelta();
			try {
				QuickAssistDeltaVisitor quickAssistDeltaVisitor = new QuickAssistDeltaVisitor(resource);
				delta.accept(quickAssistDeltaVisitor);
				if (quickAssistDeltaVisitor.isFound()) {
					refreshLinesWithAssist();
				}
			} catch (CoreException e) {
				Debug.FRAMEWORK.log(e);
			}
		}
	}

	/**
	 * This class visits a set of resource changes until it confirms that a
	 * particular resource has changed. The
	 * {@link QuickAssistDeltaVisitor#isFound()} method can be used to get the
	 * answer.
	 */
	private class QuickAssistDeltaVisitor implements IResourceDeltaVisitor {

		/**
		 * The resource to look for.
		 */
		private IResource resource;

		/**
		 * True if the resource changed, false otherwise. This value is
		 * initialized to false.
		 */
		private boolean found;

		/**
		 * Constructor for this.
		 *
		 * @param resource
		 *            The resoruce to look for. This value will not be null.
		 */
		public QuickAssistDeltaVisitor(IResource resource) {
			this.resource = resource;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean visit(IResourceDelta arg0) throws CoreException {
			boolean continueVisiting = true;
			if (arg0.getResource().equals(resource)) {
				found = true;
				continueVisiting = false;
			}

			return continueVisiting;
		}

		/**
		 * Returns whether the resource was found in the resource delta
		 * hierarchy.
		 *
		 * @return True if the resource was found, false otherwise.
		 */
		public boolean isFound() {
			return found;
		}

	}
}
