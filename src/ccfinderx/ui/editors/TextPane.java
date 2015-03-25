package ccfinderx.ui.editors;

import gnu.trove.TIntArrayList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;

import ccfinderx.model.CcfxDetectionOptions;
import ccfinderx.model.ClonePair;
import ccfinderx.model.CodeFragment;
import ccfinderx.model.Model;
import ccfinderx.model.SourceFile;
import ccfinderx.resources.TextColors;
import ccfinderx.utilities.BitArray;
import ccfinderx.utilities.Decoder;
import ccfinderx.utilities.PrepReader;
import ccfinderx.utilities.PrepReaderError;
import ccfinderx.utilities.PrepToken;

public class TextPane
{
	private Shell shell;
	private Composite sc;
	private Label fileNameLabel;

	private Composite lineNumberAndText;
	private StyledText text;
	private Canvas lineNumber;

	private final ArrayList<TextPaneScrollListener> listeners = new ArrayList<TextPaneScrollListener>();
	
	private Model viewedModel;

	private int initialTopPosition;
	private int initialTokenPosition;
	
	private int fileIndex;
	private SourceFile file;
	private String textString;
	private String textStringLower;
	private PrepToken[] tokens;
	private int[] tokenEndIndices;
	private ClonePair[] clonePairs;
	private int[] selectedClonePairs;
	private int[] lineStatus;

	private static final int WHOLE_LINE_COVERED = 1 << 0;
	private static final int CLONE_BEGIN_LINE = 1 << 1;
	private static final int CLONE_END_LINE = 1 << 2;
	private static final int WHOLE_LINE_SELECTED = 1 << 3;
	private static final int SELECTION_BEGIN_LINE = 1 << 4;
	private static final int SELECTION_END_LINE = 1 << 5;
	private static final int BETWEEN_FILE_SHIFT = 6;

	private int bottomDisplayHeight = 5; // teketo-
	
	private String encodingName = "";

	private long[] allCloneSetIDsSelectedByRightClick;
	private long[] innerfileCloneSetIDsSelectedByRightClick;
	private long[] bothCloneSetIDsSelectedByRightClick;
	private long[] crossfileCloneSetIDsSelectedByRightClick;
	
	private int searchingIndex = -1;
	private String searchingText = null;

	public void clearInitalTopPosition() {
		this.initialTopPosition = -1; // -1 means "not initialized"
	}
	
	private class ScrollRequest {
		private int cloneIndex;
		public ScrollRequest(int cloneIndex) {
			this.cloneIndex = cloneIndex;
		}
		public void run() {
			text.setTopIndex(this.cloneIndex);
		}
	}
	private ScrollRequest textScrollRequest = null;
	
	public void addListener(int eventType, Listener listener) {
		assert eventType == SWT.FocusIn;
		this.text.addListener(eventType, listener);
	}
	
	public String getEncoding() {
		return this.encodingName; // may return null
	}
	
	public boolean setEncoding(String encodingName) {
		if (encodingName == null) {
			this.encodingName = "";
			return true;
		}
		
		this.encodingName = encodingName;
		if (! Decoder.isValidEncoding(encodingName)) {
			return false;
		}
		return true;
	}
	
	public TextPane(Composite parent)
	{
		createPartControl(parent);
	}

	private int calcWidthOfNumberString(int value) {
		int nineValue = 0;
		for (int v = value; v != 0; v = v / 10) {
			nineValue = nineValue * 10 + 9;
		}
		if (nineValue < 999999) {
			nineValue = 999999;
		}
		GC gc = new GC(this.lineNumber);
		Point size = gc.textExtent(String.valueOf(nineValue));
		gc.dispose();
		return size.x;
	}

	/**
	 * @wbp.parser.entryPoint
	 */
	public void createPartControl(Composite parent)
	{
		sc = new Composite(parent, SWT.NONE);
		{
			GridLayout layout = new GridLayout(1, false);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			sc.setLayout(layout);
		}

		fileNameLabel = new Label(sc, SWT.LEFT);
		fileNameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		fileNameLabel.setText("-"); //$NON-NLS-1$
		fileNameLabel.setToolTipText(""); //$NON-NLS-1$

		lineNumberAndText = new Composite(sc, SWT.NONE);
		lineNumberAndText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		{
			GridLayout layout = new GridLayout(2, false);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			lineNumberAndText.setLayout(layout);
		}
		lineNumberAndText.setBackground(TextColors.getWhite());

		lineNumber = new Canvas(lineNumberAndText, SWT.NONE);
		{
			int width = calcWidthOfNumberString(999999);
			GridData gridData = new GridData(SWT.NONE, SWT.FILL, false, true);
			gridData.widthHint = width;
			gridData.heightHint = 200;
			lineNumber.setLayoutData(gridData);
		}

		text = new StyledText(lineNumberAndText, SWT.H_SCROLL | SWT.V_SCROLL);

		text.setForeground(TextColors.getNeglectedText());
		{
			GridData gridData = new GridData();
			gridData.horizontalAlignment = GridData.FILL;
			gridData.grabExcessHorizontalSpace = true;
			gridData.verticalAlignment = GridData.FILL;
			gridData.grabExcessVerticalSpace = true;
			text.setLayoutData(gridData);
		}
		text.setEditable(false);
		text.setText(""); //$NON-NLS-1$

		ScrollBar bar = text.getVerticalBar();
	}

	private static String toPrepFilePath(String path, String[] prepDirs) {
		for (int i = 0; i < prepDirs.length; ++i) {
			String prepDir = prepDirs[i];
			if (path.startsWith(prepDir)) {
				if (path.length() > prepDir.length() && path.charAt(prepDir.length()) == File.separatorChar) {
					return prepDir + File.separator + ".ccfxprepdir" + path.substring(prepDir.length()); //$NON-NLS-1$
				}
			}
		}
		return path;
	}

	private void set_line_status(int index, ClonePair p, int status) {
		lineStatus[index] |= (p.leftFile == p.rightFile) ? status : (status << BETWEEN_FILE_SHIFT);
	}
	private void set_line_status(int beginIndex, int endIndex, ClonePair p, int status) {
		int value = (p.leftFile == p.rightFile) ? status : (status << BETWEEN_FILE_SHIFT);
		for (int i = beginIndex; i < endIndex; ++i) {
			lineStatus[i] |= value;
		}
	}

	private static boolean areWhiteSpaces(String str, int begin, int end) {
		for (int i = begin; i < end; ++i) {
			char ch = str.charAt(i);
			switch (ch) {
				case ' ':
				case '\t':
				case '\r':
				case '\n':
				case '\f':
					break;
				default:
					return false;
			}
		}
		return true;
	}

	private static int findNoWhiteSpace(String str, int begin) {
		int i = begin;
		while (i < str.length()) {
			char ch = str.charAt(i);
			switch (ch) {
				case ' ':
				case '\t':
				case '\r':
				case '\n':
				case '\f':
					break;
				default:
					return i;
			}
			++i;
		}
		return i;
	}

	private boolean[] appearingOnlyWhitespacesBeforeToken() {
		final String sourceText = textString != null ? textString : "";
		boolean[] values = new boolean[tokens.length];
		for (int i = 0; i < tokens.length - 1; ++i) {
			final PrepToken curToken = tokens[i];
			final PrepToken nextToken = tokens[i + 1];
			if (areWhiteSpaces(sourceText, curToken.endIndex, nextToken.beginIndex)) {
				values[i] = true;
			}
		}
		return values;
	}

	private void setTokenRangeColor(int begin, int end, Color bgcolor,
			boolean[] appearingOnlyWhitespacesBeforeTokenData) {
		StyleRange negStyleRange = new StyleRange();
		negStyleRange.foreground = TextColors.getNeglectedText();
		negStyleRange.background = bgcolor;

		StyleRange resStyleRange = new StyleRange();
		resStyleRange.foreground = TextColors.getReservedWord();
		resStyleRange.fontStyle = SWT.BOLD;
		resStyleRange.background = bgcolor;

		StyleRange txtStyleRange = new StyleRange();
		txtStyleRange.foreground = TextColors.getBlack();
		txtStyleRange.background = bgcolor;

		final String sourceText = textString != null ? textString : "";
		int textSize = text.getCharCount();
		int charIndex = tokens[begin].beginIndex;
		int i = begin;
		while (i < end) {
			final PrepToken token = tokens[i];
			int tokenEndIndex = token.endIndex;
			if (tokenEndIndex > textSize) {
				break; // while
			}
			//int line = text.getLineAtOffset(token.beginIndex);
			if (charIndex < token.beginIndex) {
				StyleRange styleRange = (StyleRange)negStyleRange.clone();
				styleRange.start = charIndex;
				styleRange.length = token.beginIndex - charIndex;
				text.setStyleRange(styleRange);
			}
			int len = 1;
			{
				PrepToken nextToken;
				while (i + len < end && (nextToken = tokens[i + len]).isReservedWord == token.isReservedWord
						&& appearingOnlyWhitespacesBeforeTokenData[i + len - 1]) {
					tokenEndIndex = nextToken.endIndex;
					++len;
				}
			}
			final StyleRange styleRange = token.isReservedWord ? (StyleRange)resStyleRange.clone() : (StyleRange)txtStyleRange.clone();
			styleRange.start = tokens[i].beginIndex;
			int endIndex = (i + len < end) ? findNoWhiteSpace(sourceText, tokens[i + len - 1].endIndex)
					: tokens[i + len - 1].endIndex;
			styleRange.length = endIndex - tokens[i].beginIndex;
			text.setStyleRange(styleRange);
			charIndex = endIndex;
			i += len;
		}

		final PrepToken beginToken = tokens[begin];
		final PrepToken endToken = tokens[end - 1];
		int beginLine = text.getLineAtOffset(beginToken.beginIndex);
		int endLine = text.getLineAtOffset(endToken.endIndex - 1);
		text.setLineBackground(beginLine, endLine - beginLine, bgcolor);
	}

	private void setTextHighlightsAndLineStatus(boolean updateSelectionOnly) {
		if (textString == null || textString.length() == 0 || tokens == null) {
			return;
		}

		final int strLength = textString.length();
		//final boolean updateUnselected = ! updateSelectionOnly;

		final boolean[] appearingOnlyWhitespacesBeforeTokenData = appearingOnlyWhitespacesBeforeToken();
		setTokenRangeColor(0, tokens.length, TextColors.getWhite(), appearingOnlyWhitespacesBeforeTokenData);
		{
			{
				BitArray tokenDones = new BitArray(tokens.length * 3);
				for (int i = 0; i < clonePairs.length; ++i) {
					ClonePair p = clonePairs[i];
					if (Arrays.binarySearch(selectedClonePairs, i) < 0) {
						if (p.leftFile == fileIndex) {
							if (0 <= p.leftBegin && p.leftBegin < p.leftEnd && p.leftEnd <= tokens.length) {
								tokenDones.fill(p.leftBegin * 3 + 1, p.leftEnd * 3 - 1, true);
							}
						}
					}
				}
				{
					int i = tokenDones.find(true);
					while (i >= 0) {
						int j = tokenDones.find(false, i);
						if (j < 0) {
							j = tokenDones.length();
						}

						// here, tokenDones[i] .. tokenDones[j - 1] are true. tokenDones[j] = false

						setTokenRangeColor(i / 3, j / 3, TextColors.getClonePair(), appearingOnlyWhitespacesBeforeTokenData);
						i = tokenDones.find(true, j);
					}
				}
			}

			{
				Arrays.fill(lineStatus, 0);
				for (int i = 0; i < clonePairs.length; ++i) {
					boolean selected = Arrays.binarySearch(selectedClonePairs, i) >= 0;
					final ClonePair p = clonePairs[i];
					if (p.leftFile == fileIndex) {
						if (0 <= p.leftBegin && p.leftBegin < p.leftEnd && p.leftEnd <= tokens.length) {
							int beginPos = tokens[p.leftBegin].beginIndex;
							int endPos = tokens[p.leftEnd - 1].endIndex;

							if (0 <= beginPos && beginPos < endPos && endPos <= strLength) {
								if (!(endPos < strLength)) {
									endPos = strLength - 1;
								}
								int beginLine = text.getLineAtOffset(beginPos);
								int endLine = text.getLineAtOffset(endPos - 1);
								if (beginLine == endLine) {
									set_line_status(beginLine, p, WHOLE_LINE_COVERED | (selected ? WHOLE_LINE_SELECTED : 0));
								}
								else {
									set_line_status(beginLine, p, CLONE_BEGIN_LINE | (selected ? SELECTION_BEGIN_LINE : 0));
									set_line_status(beginLine + 1, endLine, p, WHOLE_LINE_COVERED | (selected ? WHOLE_LINE_SELECTED : 0));
									set_line_status(endLine, p, CLONE_END_LINE | (selected ? SELECTION_END_LINE : 0));
								}
							}
						}
					}
				}
			}

			for (int i = 0; i < selectedClonePairs.length; ++i) {
				final ClonePair p = clonePairs[selectedClonePairs[i]];
				if (p.leftFile == fileIndex) {
					if (0 <= p.leftBegin && p.leftBegin < p.leftEnd && p.leftEnd <= tokens.length) {
						setTokenRangeColor(p.leftBegin, p.leftEnd, TextColors.getSelectedClonePair(), appearingOnlyWhitespacesBeforeTokenData);
					}
				}
			}
		}
	}

	public Control getControl() {
		return text;
	}

	public PrepToken[] getTokens(int fileIndex) {
		if (fileIndex == this.fileIndex) {
			return tokens;
		}
		
		return null;
	}
	
	public ClonePair[] getClonePairs(int fileIndex) {
		if (fileIndex == this.fileIndex) {
			return clonePairs;
		}
		
		return null;
	}
	
	public void updateModel(Model data) {
		viewedModel = data;
		
		fileIndex = -1;
		clonePairs = null;
		selectedClonePairs = null;
		lineStatus = null;
		text.setText(""); //$NON-NLS-1$
		textString = textStringLower = null;
		tokens = null;
		tokenEndIndices = null;
		innerfileCloneSetIDsSelectedByRightClick = null;
		bothCloneSetIDsSelectedByRightClick = null;
		crossfileCloneSetIDsSelectedByRightClick = null;
		allCloneSetIDsSelectedByRightClick = null;
		
		searchingIndex = -1;
		searchingText = null;
		
		int newFileIndex = file != null ? data.findFile(file) : -1;
		if (newFileIndex >= 0) {
			setFile(newFileIndex);
		} else {
			fileNameLabel.setText("-"); //$NON-NLS-1$
			fileNameLabel.setToolTipText(""); //$NON-NLS-1$
			//GC gc = new GC(lineNumber);
			//try {
			//	redrawLineNumber(gc, true);
			//} finally {
			//	gc.dispose();
			//}
		}
	}

	private String readSourceFile(String path) throws FileNotFoundException,
	IOException {
		final File file = new File(path);
		final InputStream inp = new FileInputStream(file);
		final BufferedInputStream inpBuf = new BufferedInputStream(inp);
		if (encodingName.length() == 0) {
			Reader reader = new InputStreamReader(inpBuf, "UTF8"); //$NON-NLS-1$
			StringWriter writer = new StringWriter();
			int data;
			while ((data = reader.read()) != -1) {
				writer.write(data);
			}
			writer.flush();
			String str = writer.toString();
			reader.close();
			writer.close();
			return str;
		} else {
			final byte[] seq = new byte[inpBuf.available()];
			inpBuf.read(seq);
			inp.close();

			return Decoder.decode(seq, encodingName);
		}
	}

	public int[] getViewedFiles() {
		if (fileIndex != -1) {
			return new int[] { fileIndex };
		} else {
			return new int[0];
		}
	}
	
	public static class BeginEnd {
		public final int begin;
		public final int end;
		public BeginEnd(int begin, int end) {
			this.begin = begin;
			this.end = end;
		}
	}
	
	public void setVisibleTokenCenterIndex(int tokenIndex) {
		if (tokens == null || tokenEndIndices == null) {
			return;
		}
		
		boolean textVisible = text.isVisible();
		if (textVisible) {
			text.setVisible(false);
			lineNumber.setVisible(false);
		}
		try {
			if (tokenIndex < 0) {
				tokenIndex = 0;
			} else if (tokenIndex >= tokenEndIndices.length) {
				tokenIndex = tokenEndIndices.length - 1;
			}
			
			int charIndex = tokenEndIndices[tokenIndex];
			
			int lineIndex = text.getLineAtOffset(charIndex);
			
			final int height = text.getLineHeight();
			final int lineCount = text.getClientArea().height / height;
			
			int topLineIndex = lineIndex - lineCount / 2;
			if (topLineIndex < 0) {
				topLineIndex = 0;
			} else if (topLineIndex >= text.getLineCount()) {
				topLineIndex = text.getLineCount() - 1;
			}
			
			text.setTopIndex(topLineIndex);
			for (TextPaneScrollListener listener : TextPane.this.listeners) {
				listener.textScrolled();
			}
		}
		finally {
			if (textVisible) {
				text.setVisible(true);
				lineNumber.setVisible(true);
			}
		}
	}
	
	public BeginEnd getVisibleTokenRange() {
		if (tokens == null) {
			return null;
		}
		
		if (tokenEndIndices == null) {
			tokenEndIndices = new int[tokens.length];
			for (int i = 0; i < tokens.length; ++i) {
				tokenEndIndices[i] = tokens[i].endIndex;
			}
		}
		
		int topLine = text.getTopIndex();
		int topCharIndex = text.getOffsetAtLine(topLine);
		final int height = text.getLineHeight();
		final int lineCount = text.getClientArea().height / height;
		int bottomLine= topLine + lineCount;
		int bottomCharIndex = bottomLine < text.getLineCount() ? text.getOffsetAtLine(bottomLine) : text.getCharCount();
		
		int topTokenIndex = -1;
		int bottomTokenIndex = -1;
		int i = Arrays.binarySearch(tokenEndIndices, topCharIndex);
		if (i < 0) {
			i = -(i + 1);
		}
		if (i >= tokens.length) {
			i = tokens.length;
		}
		topTokenIndex = i;
		if (bottomLine >= text.getLineCount()) {
			return new BeginEnd(topTokenIndex, tokens.length);
		}
		i = Arrays.binarySearch(tokenEndIndices, bottomCharIndex);
		if (i < 0) {
			i = -(i + 1);
		}
		if (i >= tokens.length) {
			i = tokens.length;
		}
		bottomTokenIndex = i;
		return new BeginEnd(topTokenIndex, bottomTokenIndex);
	}

	public void addScrollListener(TextPaneScrollListener listener) {
		this.listeners.add(listener);
	}
	
	private static int[] downTrianglePath(int x, int y, int width, int height) {
		return new int[] { 
				x, y, 
				x + width / 2,  y + height / 3,
				x + width, y
		};
	}
	private static int[] upTrianglePath(int x, int y, int width, int height) {
		return new int[] { 
				x, y + height,
				x + width / 2, y + height - height / 3,
				x + width, y + height
		};
	}
	
	private void redrawLineNumber(GC gc, boolean selectionChanged) {
		final Color white = TextColors.getWhite();
		final Color black = TextColors.getBlack();
		int charHeight = gc.getFontMetrics().getHeight();
		Rectangle clientRect = lineNumber.getClientArea();
		
		{
			ScrollBar bar = text.getHorizontalBar();
			bottomDisplayHeight = bar.getSize().y;
		}
		
		boolean[] cloneExists = new boolean[] { false, false };
		if (textString != null && textString.length() > 0) {
			int lineNumberCount = text.getLineCount();
			for (int i = 0; i < lineNumberCount; ++i) {
				if (lineStatus != null) {
					if ((lineStatus[i] & (WHOLE_LINE_COVERED | CLONE_END_LINE | CLONE_BEGIN_LINE)) != 0) {
						cloneExists[0] = true;
					}
					if ((lineStatus[i] & ((WHOLE_LINE_COVERED | CLONE_END_LINE | CLONE_BEGIN_LINE) << BETWEEN_FILE_SHIFT)) != 0) {
						cloneExists[1] = true;
					}
				}
			}
		}
		{
			final int w1 = clientRect.width / 2;
			final int w2 = clientRect.width - w1;
			
			int y = -text.getTopPixel();
			gc.setLineStyle(SWT.LINE_SOLID);
			gc.setLineWidth(1);
			if (textString != null && textString.length() > 0) {
				int lineNumberCount = text.getLineCount();
				for (int i = 0; i < lineNumberCount; ++i) {
					if (0 <= y + charHeight && y < clientRect.height) {
						int num = i + 1;
						String str = String.valueOf(num);
						Point extent = gc.stringExtent(str);
						if (lineStatus != null) {
							int lsi = lineStatus[i];
							int x1 = w1;
							int x2 = w2;
							for (int t = 0; t < 2; ++t) {
								if ((lsi & WHOLE_LINE_COVERED) != 0) {
									if ((lsi & WHOLE_LINE_SELECTED) != 0) {
										gc.setBackground(TextColors.getSelectedClonePair());
										gc.fillRectangle(x1, y, x2, charHeight);
										
										if ((lsi & CLONE_END_LINE) != 0) {
											gc.setForeground(white);
											gc.drawPolyline(downTrianglePath(x1, y, x2, charHeight));
										}
										if ((lsi & CLONE_BEGIN_LINE) != 0) {
											gc.setForeground(white);
											gc.drawPolyline(upTrianglePath(x1, y, x2, charHeight));
										}
									} else {
										gc.setBackground(TextColors.getClonePair());
										gc.fillRectangle(x1, y, x2, charHeight);
										
										gc.setBackground(TextColors.getSelectedClonePair());
										gc.setForeground(white);
										if ((lsi & CLONE_END_LINE) != 0) {
											gc.setForeground(white);
											gc.drawPolyline(downTrianglePath(x1, y, x2, charHeight));
										}
										if ((lsi & CLONE_BEGIN_LINE) != 0) {
											gc.setForeground(white);
											gc.drawPolyline(upTrianglePath(x1, y, x2, charHeight));
										}
										if ((lsi & SELECTION_END_LINE) != 0) {
											final int[] line = downTrianglePath(x1, y, x2, charHeight);
											gc.fillPolygon(line);
										}
										if ((lsi & SELECTION_BEGIN_LINE) != 0) {
											final int[] line = upTrianglePath(x1, y, x2, charHeight);
											gc.fillPolygon(line);
										}
									}
								} else {
									gc.setBackground(white);
									gc.fillRectangle(x1, y, x2, charHeight);
									if ((lsi & CLONE_END_LINE) != 0) {
										gc.setBackground((lsi & SELECTION_END_LINE) != 0 ? 
												TextColors.getSelectedClonePair() : TextColors.getClonePair());
										final int[] line = downTrianglePath(x1, y, x2, charHeight);
										gc.fillPolygon(line);
									}
									if ((lsi & CLONE_BEGIN_LINE) != 0) {
										gc.setBackground((lsi & SELECTION_BEGIN_LINE) != 0 ? 
												TextColors.getSelectedClonePair() : TextColors.getClonePair());
										final int[] line = upTrianglePath(x1, y, x2, charHeight);
										gc.fillPolygon(line);
									}
								}
								x1 = 0;
								x2 = w1;
								lsi >>>= BETWEEN_FILE_SHIFT;
							}
						} else {
							gc.setBackground(white);
						}
						gc.setForeground(black);
						gc.drawText(str, clientRect.width - extent.x, y, SWT.DRAW_TRANSPARENT);
						boolean inTheInitialPosition = initialTopPosition != -1 && i == initialTopPosition;
						if (inTheInitialPosition) {
							Point ex = gc.stringExtent("M"); //$NON-NLS-1$
							String s = "i"; //$NON-NLS-1$
							gc.setBackground(black);
							gc.fillRectangle(0, y, ex.x, charHeight);
							gc.setForeground(white);
							gc.drawText(s, (ex.x - 0) / 2, y, SWT.NONE);
						}
					}
					y += charHeight;
				}
			}
		}
		
		final int w1 = clientRect.width / 2;
		final int w2 = clientRect.width - w1;
		int y = -text.getTopPixel() + text.getLineCount() * charHeight;
		if (y < clientRect.height - bottomDisplayHeight) {
			gc.setBackground(white);
			gc.fillRectangle(0, y, clientRect.width, clientRect.height - bottomDisplayHeight - y);
		}
		{
			gc.setBackground(white);
			int y0 = clientRect.height - bottomDisplayHeight;
			gc.fillRectangle(0, y0, clientRect.width, bottomDisplayHeight);
			gc.setBackground(TextColors.getClonePair());
			final int margin = 2;
			if (cloneExists[0]) {
				gc.fillRectangle(w1 + margin, y0 + margin, w2 - margin * 2, bottomDisplayHeight - margin * 2);
			}
			if (cloneExists[1]) {
				gc.fillRectangle(0 + margin, y0 + margin, w1 - margin * 2, bottomDisplayHeight - margin * 2);
			}
		}
	}
	
	private void setFile(int fileIndex) {
		this.fileIndex = fileIndex;
		file = viewedModel.getFile(fileIndex);
		String filenamestr = String.valueOf(file.id) + " " + file.path; //$NON-NLS-1$
		fileNameLabel.setText(filenamestr); //$NON-NLS-1$
		fileNameLabel.setToolTipText(filenamestr);
		CcfxDetectionOptions options = viewedModel.getDetectionOption();
		String postfix = options.getPostfix();
		String prepDirs[] = options.get("n"); //$NON-NLS-1$
		if (postfix == null) {
			postfix = "." + viewedModel.getPreprocessScript() + ".ccfxprep"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		tokenEndIndices = null;

		String prepFilePath = toPrepFilePath(file.path, prepDirs);

		try {
			tokens = (new PrepReader()).read(prepFilePath, postfix);
		} catch (PrepReaderError e) {
			tokens = null;
		} catch (IOException e) {
			tokens = null;
		}

		selectedClonePairs = new int[0];
		lineStatus = null;

		boolean lineNumberAndTextVisible = lineNumberAndText.isVisible();
		if (lineNumberAndTextVisible) {
			lineNumberAndText.setVisible(false);
		}

		searchingIndex = -1;
		searchingText = null;

		try {
			String str = readSourceFile(file.path);
			if (tokens != null && tokens[tokens.length - 1].endIndex > str.length()) {
				tokens = null; // maybe the encoding used to generate the clone data is not the same as the encoding to the current encoding
			}
			textString = str;
			textStringLower = textString.toLowerCase();
			text.setText(str);
			lineStatus = new int[text.getLineCount() + 1];
			//Point size = lineNumber.computeSize(lineNumberWidth, SWT.DEFAULT);
			{
				int width = calcWidthOfNumberString(text.getLineCount() + 1);
				GridData gridData = new GridData(SWT.NONE, SWT.FILL, false, true);
				gridData.widthHint = width;
				gridData.heightHint = 200;
				lineNumber.setLayoutData(gridData);
			}
			this.lineNumberAndText.layout();

			clonePairs = viewedModel.getClonePairsOfFile(fileIndex).clone();
			Arrays.sort(clonePairs);

			setTextHighlightsAndLineStatus(false);
			//rebuildLineNumberImage();
		} catch (FileNotFoundException e) {
			textString = textStringLower = ""; //$NON-NLS-1$
			text.setText(textString); //$NON-NLS-1$
		} catch (IOException e) {
			textString = textStringLower = ""; //$NON-NLS-1$
			text.setText(textString); //$NON-NLS-1$
		} finally {
			if (lineNumberAndTextVisible) {
				lineNumberAndText.setVisible(true);
			}
		}
	}

	public void setSelection(int[] fileIndices) {
		if (viewedModel != null && fileIndices != null && fileIndices.length == 1) {
			if (fileIndex != fileIndices[0]) {
				setFile(fileIndices[0]);
				initialTopPosition = 0;
				initialTokenPosition = 0;
			}
		} else {
			fileIndex = -1;
			clonePairs = null;
			selectedClonePairs = null;
			lineStatus = null;
			text.setText(""); //$NON-NLS-1$
			textString = textStringLower = null;
			tokens = null;
			tokenEndIndices = null;
			
			initialTopPosition = 0;
			initialTokenPosition = 0;
			
			searchingIndex = -1;
			searchingText = null;
			
			fileNameLabel.setText("-"); //$NON-NLS-1$
			fileNameLabel.setToolTipText(""); //$NON-NLS-1$
			GC gc = new GC(lineNumber);
			try {
				this.redrawLineNumber(gc, true);
			} finally {
				gc.dispose();
			}
		}
	}
	
	public void setCodeFragmentSelection(CodeFragment selectedCodeFragment, long cloneSetID) {
		long[] cloneSetIDs = new long[] { cloneSetID };
		setCloneSelection(cloneSetIDs, selectedCodeFragment, true);
	}
	
	public void setCloneSelection(long[] cloneSetIDs) {
		setCloneSelection(cloneSetIDs, null, true);
	}
	
	private void setCloneSelection(long[] cloneSetIDs, CodeFragment targetCodeFragment, boolean scrollToIt) {
		if (clonePairs == null) {
			selectedClonePairs = null;
			return;
		}
		
		long[] ids = cloneSetIDs.clone();
		Arrays.sort(ids);
		
		int[] clonePairIndices;
		{
			TIntArrayList cpis = new TIntArrayList();
			for (int j = 0; j < clonePairs.length; ++j) {
				ClonePair p = clonePairs[j];
				if (Arrays.binarySearch(ids, p.classID) >= 0) {
					cpis.add(j);
				}
			}
			clonePairIndices = cpis.toNativeArray();
			Arrays.sort(clonePairIndices);
		}

		boolean selectedClonePairUnchanged = true;
		if (clonePairIndices.length != selectedClonePairs.length) {
			selectedClonePairUnchanged = false;
		}
		else {
			for (int i = 0; i < clonePairIndices.length; ++i) {
				if (clonePairIndices[i] != selectedClonePairs[i]) {
					selectedClonePairUnchanged = false;
					break; // for i
				}
			}
		}
		if (! selectedClonePairUnchanged) {
			selectedClonePairs = clonePairIndices;
		}
		
		boolean textVisible = text.isVisible();
		if (textVisible) {
			text.setVisible(false);
			lineNumber.setVisible(false);
		}
		try {
			if (! selectedClonePairUnchanged) {
				setTextHighlightsAndLineStatus(true);
				//rebuildLineNumberImage();
			}
			
			if (tokens != null && scrollToIt) {
				if (targetCodeFragment != null && targetCodeFragment.file == fileIndex) {
					// move caret to the left code fragment of the clone pair
					//int curIndex = text.getTopIndex();
					int cloneIndex = text.getLineAtOffset(tokens[targetCodeFragment.begin].beginIndex);
					
					TextPane.this.textScrollRequest = new ScrollRequest(cloneIndex);
					//text.setTopIndex(cloneIndex);
					
					if (initialTopPosition == -1) {
						initialTopPosition = cloneIndex;
						initialTokenPosition = targetCodeFragment.begin;
					}
				} else {
					// move caret to the nearest clone
					int curIndex = text.getTopIndex();
					int nearestCloneIndex = -1;
					int nearestCloneTokenPosition = -1;
					int minDistance = Integer.MAX_VALUE;
					for (int i = 0; i < clonePairIndices.length; ++i) {
						ClonePair pair = clonePairs[clonePairIndices[i]];
						if (pair.leftFile == fileIndex) {
							int cloneIndex = text.getLineAtOffset(tokens[pair.leftBegin].beginIndex);
							int distance = cloneIndex - curIndex;
							if (distance < 0) {
								if (-distance < minDistance) {
									nearestCloneIndex = cloneIndex;
									nearestCloneTokenPosition = pair.leftBegin;
									minDistance = -distance;
								}
							} else {
								if (distance < minDistance) {
									nearestCloneIndex = cloneIndex;
									nearestCloneTokenPosition = pair.leftBegin;
									minDistance = distance;
								}
							}
						}
					}
					if (nearestCloneIndex != -1) {
						//text.setTopIndex(nearestCloneIndex);
						TextPane.this.textScrollRequest = new ScrollRequest(nearestCloneIndex);
						
						if (initialTopPosition == -1) {
							initialTopPosition = nearestCloneIndex;
							initialTokenPosition = nearestCloneTokenPosition;
						}
					}
				}
			}
		} finally {
			if (textVisible) {
				text.setVisible(true);
				lineNumber.setVisible(true);
			}
		}
	}
}
