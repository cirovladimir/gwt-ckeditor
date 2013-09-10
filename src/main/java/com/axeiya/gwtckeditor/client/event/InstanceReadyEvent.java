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
package com.axeiya.gwtckeditor.client.event;

import com.axeiya.gwtckeditor.client.CKEditor;
import com.google.gwt.event.shared.GwtEvent;

public class InstanceReadyEvent extends GwtEvent<InstanceReadyHandler> {
	
	public static Type<InstanceReadyHandler> TYPE = new Type<InstanceReadyHandler>();

	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<InstanceReadyHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(InstanceReadyHandler handler) {
		handler.onInstanceReady(this);
	}
	
	/**
     * Fires an instanceReadyEvent on all registered handlers in the handler manager
     *
     * @param source the source of the handlers
     * @param target the editor instance that triggered the event
     */
	public static void fire(HasInstanceReadyHandlers source, CKEditor target) {
			InstanceReadyEvent event = new InstanceReadyEvent(target);
			source.fireEvent(event);
	}
	
	private CKEditor editor;
	
	public InstanceReadyEvent(CKEditor editor) {
		this.editor = editor;
	}
	
	/**
	 * @return The editor that triggered the event
	 */
	public CKEditor getEditor() {
		return editor;
	}

}
