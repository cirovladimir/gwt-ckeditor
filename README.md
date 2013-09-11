gwt-ckeditor
============

This project aims to wrap CKEditor (http://ckeditor.com/) with GWT. Forked from https://code.google.com/p/gwt-ckeditor/

I use gwt-ckeditor on [SmartGwt](https://code.google.com/p/smartgwt/), so changes here are intended for better integration with it.

Basic Usage
--------------------
	public class Desktop implements EntryPoint {
		private static final int CKEDITOR_ZINDEX = 1000000; // any value greater than z-index of SmartGwt controls
		@Override
		public void onModuleLoad() {
			CKConfig config = new CKConfig();
			config.setBaseFloatZIndex(CKEDITOR_ZINDEX);
			config.setUseFormPanel(false);
			config.setAutoSaveLatencyInMillis(3*1000); // time to wait for Save Event to be fired after key pressed on editor
			CKEditor ckeEditor = new CKEditor(config);
			ckeEditor.addInstanceReadyHandler(new InstanceReadyHandler() {
				@Override
				public void onInstanceReady(InstanceReadyEvent event) {
					event.getEditor().setHTML("Load initial contents here...");
				}
			});
			ckeEditor.addSaveHandler(new SaveHandler<CKEditor>() {
				@Override
				public void onSave(SaveEvent<CKEditor> event) {
					// send contents to server, event.getTarget().getHTML()
				}
			});
			ckeEditor.setSize("100%", "100%");
			canvas = new Canvas();
			canvas.addChild(ckeEditor);
			canvas.setSize("100%", "100%");
			canvas.show();
		}
	}
