/**
 *  This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axeiya.gwtckeditor.client;

import com.axeiya.gwtckeditor.client.event.HasSaveHandlers;
import com.axeiya.gwtckeditor.client.event.SaveEvent;
import com.axeiya.gwtckeditor.client.event.SaveHandler;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;

/**
 * This class provides a CKEdtior as a Widget
 * 
 * @author Damien Picard <damien.picard@axeiya.com>
 * @author Emmanuel COQUELIN <emmanuel.coquelin@axeiya.com>
 */
public class CKEditor extends Composite implements HasSaveHandlers<CKEditor>, HasValueChangeHandlers<String>, ClickHandler, HasAlignment, HasHTML,
		HasText {

	/**
	 * Used for catching Save event
	 * 
	 * @param o
	 * @return
	 */
	private static native String getParentClassname(JavaScriptObject o) /*-{
		var classname = o.parentNode.getAttribute("class");
		if (classname == null)
			return o.parentNode.className;
		return classname;
	}-*/;

	private static class AutoSaveTimer extends Timer {

		protected CKEditor editor;

		public AutoSaveTimer(CKEditor editor) {
			this.editor = editor;
		}

		@Override
		public void run() {
			editor.save();
		}

		public void delay() {
			this.cancel();
			if (editor.config.getAutoSaveLatencyInMillis() > 0) {
				this.schedule(editor.config.getAutoSaveLatencyInMillis());
			}
		}

	}

	protected String name;
	protected JavaScriptObject editor;
	protected TextArea textArea;
	protected Element baseTextArea;
	protected JavaScriptObject dataProcessor;
	protected CKConfig config;
	protected boolean replaced = false;
	protected boolean textWaitingForAttachment = false;
	protected String waitingText;
	protected boolean waitingForDisabling = false;
	protected boolean disabled = false;
	protected Element div;
	protected Node ckEditorNode;
	protected HTML disabledHTML;

	protected AutoSaveTimer autoSaveTimer = new AutoSaveTimer(this);

	protected boolean focused = false;
	protected HorizontalAlignmentConstant hAlign = null;

	protected VerticalAlignmentConstant vAlign = null;

	/**
	 * Creates an editor with the CKConfig.basic configuration. By default, the
	 * CKEditor is enabled in hosted mode ; if not, the CKEditor is replaced by
	 * a simple TextArea
	 */
	public CKEditor() {
		this(CKConfig.basic);
	}

	/**
	 * Creates an editor with the given configuration. By default, the CKEditor
	 * is enabled in hosted mode ; if not, the CKEditor is replaced by a simple
	 * TextArea
	 * 
	 * @param config
	 *            The configuration
	 */
	public CKEditor(CKConfig config) {
		super();
		this.config = config;
		initCKEditor();
	}

	@Override
	public HandlerRegistration addSaveHandler(SaveHandler<CKEditor> handler) {
		return addHandler(handler, SaveEvent.getType());
	}

	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
		return this.addHandler(handler, ValueChangeEvent.getType());
	}

	private native void destroyInstance()/*-{
		var editor = this.@com.axeiya.gwtckeditor.client.CKEditor::editor;
		if (editor) {
			editor.destroy();
		}
	}-*/;

	/**
	 * Dispatch a blur CKEditor event to a ValueChangeEvent
	 */
	private void dispatchBlur() {
		ValueChangeEvent.fire(this, this.getHTML());
	}

	private void dispatchKeyPressed() {
		autoSaveTimer.delay();
	}

	/**
	 * {@link #getHTML()}
	 * 
	 * @return
	 */
	public String getData() {
		return getHTML();
	}

	@Override
	public HorizontalAlignmentConstant getHorizontalAlignment() {
		return hAlign;
	}

	/**
	 * Returns the editor text
	 * 
	 * @return the editor text
	 */
	public String getHTML() {
		if (replaced)
			return getNativeHTML();
		else {
			return waitingText;
		}
	}

	private native String getNativeHTML() /*-{
		var e = this.@com.axeiya.gwtckeditor.client.CKEditor::editor;
		return e.getData();
	}-*/;

	public native JavaScriptObject getSelection() /*-{
		var e = this.@com.axeiya.gwtckeditor.client.CKEditor::editor;
		return e.getSelection();
	}-*/;

	/**
	 * Use getHTML() instead. Returns the editor text
	 * 
	 * @return the editor text
	 */
	@Deprecated
	public String getText() {
		return getNativeHTML();
	}

	@Override
	public VerticalAlignmentConstant getVerticalAlignment() {
		return vAlign;
	}

	/**
	 * Initialize the editor
	 */
	private void initCKEditor() {
		div = DOM.createDiv();
		baseTextArea = DOM.createTextArea();
		name = HTMLPanel.createUniqueId();
		div.appendChild(baseTextArea);
		DOM.setElementAttribute(baseTextArea, "name", name);
		this.sinkEvents(Event.ONCLICK | Event.KEYEVENTS);

		if (config.isUsingFormPanel()) {
			FormPanel form = new FormPanel();
			Button submit = new Button();
			submit.addClickHandler(this);
			submit.getElement().setAttribute("name", "submit");
			submit.setVisible(false);
			// .getElement().setAttribute("style", "visibility:hidden");

			form.getElement().appendChild(div);
			form.add(submit);
			initWidget(form);
		} else {
			SimplePanel simplePanel = new SimplePanel();
			simplePanel.getElement().appendChild(div);
			initWidget(simplePanel);
		}
	}

	/**
	 * Replace the text Area by a CKEditor Instance
	 */
	protected void initInstance() {
		if (!replaced && !disabled) {
			replaced = true;
			replaceTextArea(baseTextArea, this.config.getConfigObject());

			if (textWaitingForAttachment) {
				textWaitingForAttachment = false;
				setHTML(waitingText);
			}

			if (hAlign != null) {
				setHorizontalAlignment(hAlign);
			}

			if (vAlign != null) {
				setVerticalAlignment(vAlign);
			}

			if (this.config.isFocusOnStartup()) {
				this.focused = true;
				setAddFocusOnLoad(focused);
			}

			if (waitingForDisabling) {
				this.waitingForDisabling = false;
				setEnabled(this.disabled);
			}

			listenToBlur();
			listenToKey();
			/*
			 * if (config.getBreakLineChars() != null) {
			 * setNativeBreakLineChars(config.getBreakLineChars()); }
			 * 
			 * if (config.getSelfClosingEnd() != null) {
			 * setNativeSelfClosingEnd(config.getSelfClosingEnd()); }
			 */
		}

	}

	public boolean isDisabled() {
		return disabled;
	}

	private native void listenToBlur() /*-{
		var me = this;
		var e = this.@com.axeiya.gwtckeditor.client.CKEditor::editor;
		e.on('blur', function(ev) {
			me.@com.axeiya.gwtckeditor.client.CKEditor::dispatchBlur()();
		});
	}-*/;

	private native void listenToKey() /*-{
		var me = this;
		var e = this.@com.axeiya.gwtckeditor.client.CKEditor::editor;
		e.on('key', function(ev) {
			me.@com.axeiya.gwtckeditor.client.CKEditor::dispatchKeyPressed()();
		});
	}-*/;

	@Override
	public void onClick(ClickEvent event) {
		if (event.getRelativeElement().getAttribute("name").equals("submit")) {
			event.stopPropagation();
			save();
		}
	}

	/**
	 * Dispatch a save event
	 */
	protected void save() {
		SaveEvent.fire(this, this, this.getHTML());
	}

	@Override
	protected void onLoad() {
		initInstance();
	}

	private native void replaceTextArea(Object o, JavaScriptObject config) /*-{
		this.@com.axeiya.gwtckeditor.client.CKEditor::editor = $wnd.CKEDITOR
				.replace(o, config);
	}-*/;

	private native void setAddFocusOnLoad(boolean focus)/*-{
		var e = this.@com.axeiya.gwtckeditor.client.CKEditor::editor;

		e.on('dataReady', function(ev) {

			if (focus) {
				e.focus();
				var lastc = e.document.getBody().getLast();
				e.getSelection().selectElement(lastc);
				var range = e.getSelection().getRanges()[0];
				if (range != null) {
					range.collapse(false);
					range.setStart(lastc, range.startOffset);
					try {
						range.setEnd(lastc, range.endOffset);
					} catch (err) {
					}
					range.select();
				}
			}

		});
	}-*/;

	/**
	 * {@link #setHTML(String)}
	 * 
	 * @param data
	 */
	public void setData(String data) {
		setHTML(data);
	}

	/**
	 * Use to disable CKEditor's instance
	 * 
	 * @param disabled
	 */
	public void setEnabled(boolean enabled) {
		//FIXME : rework this part to remove the !
		boolean disabled = !enabled;

		if (this.disabled != disabled) {
			this.disabled = disabled;

			if (disabled) {
				ScrollPanel scroll = new ScrollPanel();
				disabledHTML = new HTML();
				disabledHTML.setStyleName("GWTCKEditor-Disabled");
				scroll.setWidget(disabledHTML);

				if (config.getWidth() != null)
					scroll.setWidth(config.getWidth());

				if (config.getHeight() != null)
					scroll.setHeight(config.getHeight());

				String htmlString = new String();

				if (replaced) {
					htmlString = getHTML();
				} else {
					htmlString = waitingText;
				}

				DivElement divElement = DivElement.as(this.getElement().getFirstChildElement());
				Node node = divElement.getFirstChild();
				while (node != null) {
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						com.google.gwt.dom.client.Element element = com.google.gwt.dom.client.Element.as(node);
						if (element.getTagName().equalsIgnoreCase("textarea")) {
							destroyInstance();
							replaced = false;
							divElement.removeChild(node);
							ckEditorNode = node;
						}
					}
					node = node.getNextSibling();
				}
				disabledHTML.setHTML(htmlString);
				div.appendChild(scroll.getElement());

			} else {
				if (ckEditorNode != null) {
					DivElement divElement = DivElement.as(this.getElement().getFirstChildElement());
					Node node = divElement.getFirstChild();
					while (node != null) {
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							com.google.gwt.dom.client.Element element = com.google.gwt.dom.client.Element.as(node);
							if (element.getTagName().equalsIgnoreCase("div")) {
								divElement.removeChild(node);

							}
						}
						node = node.getNextSibling();
					}
					div.appendChild(baseTextArea);
					initInstance();

				}
			}
		}

	}

	/**
	 * Set the focus natively if ckEditor is attached, alerts you if it's not
	 * the case.
	 * 
	 * @param focus
	 */
	public void setFocus(boolean focus) {
		if (replaced == true) {
			setNativeFocus(focus);
		} else {
			Window.alert("You can't set the focus on startup with the method setFocus(boolean focus).\n"
					+ "If you want to add focus to your instance on startup, use the config object\n"
					+ "with the method setFocusOnStartup(boolean focus) instead.");
		}
	}

	/**
	 * If you want to set the height, you must do so with the configuration
	 * object before instanciating
	 */
	@Deprecated
	@Override
	public void setHeight(String height) {
		super.setHeight(height);
	}

	@Override
	public void setHorizontalAlignment(HorizontalAlignmentConstant align) {
		this.hAlign = align;
		if (replaced)
			this.getElement().getParentElement().setAttribute("align", align.getTextAlignString());
	}

	/**
	 * Set the editor's html
	 * 
	 * @param html
	 *            The html string to set
	 */
	public void setHTML(String html) {
		if (replaced)
			setNativeHTML(html);
		else {
			waitingText = html;
			textWaitingForAttachment = true;
		}
	}

	private native void setNativeFocus(boolean focus)/*-{
		if (focus) {
			var e = this.@com.axeiya.gwtckeditor.client.CKEditor::editor;
			if (e) {
				e.focus();

				var lastc = e.document.getBody().getLast();
				e.getSelection().selectElement(lastc);
				var range = e.getSelection().getRanges()[0];
				range.collapse(false);
				range.setStart(lastc, range.startOffset);
				try {
					range.setEnd(lastc, range.endOffset);
				} catch (err) {
				}
				range.select();
			}
		}
	}-*/;

	private native void setNativeHTML(String html) /*-{
		var e = this.@com.axeiya.gwtckeditor.client.CKEditor::editor;
		e.setData(html);
	}-*/;

	/**
	 * Use setHtml(String html) instead. Set the editor text
	 * 
	 * @param text
	 *            The text to set
	 */
	@Deprecated
	public void setText(String text) {
		if (replaced)
			setNativeHTML(text);
		else {
			waitingText = text;
			textWaitingForAttachment = true;
		}
	}

	@Override
	public void setVerticalAlignment(VerticalAlignmentConstant align) {
		this.vAlign = align;
		if (replaced)
			this.getElement().getParentElement().setAttribute("style", "vertical-align:" + align.getVerticalAlignString());

	}

	/**
	 * If you want to set the width, you must do so with the configuration
	 * object before instanciating
	 */
	@Deprecated
	@Override
	public void setWidth(String width) {
		super.setWidth(width);
	}
}
