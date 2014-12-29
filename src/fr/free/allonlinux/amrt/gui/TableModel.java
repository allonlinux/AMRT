package fr.free.allonlinux.amrt.gui;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class TableModel extends AbstractTableModel
{
	private String[] columnNames;
	private Vector<Vector<Object>> data = new Vector<Vector<Object>>();
	
	public TableModel(String[] i__columnNames)
	{
		columnNames=i__columnNames;
	}

	public int getColumnCount()
	{
		return columnNames.length;
	}

	public int getRowCount()
	{
		return data.size();
	}

	public String getColumnName(int i__columnIndex)
	{
		return columnNames[i__columnIndex];
	}

	/*
	 * JTable uses this method to determine the default renderer/
	 * editor for each cell. If we didn't implement this method,
	 * then the last column would contain text ("true"/"false"),
	 * rather than a check box.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class getColumnClass(int i__columnIndex)
	{
		if ( getRowCount() > 0 )
			return getValueAt(0, i__columnIndex).getClass();
		else
			return String.class;
	}

	// Only the last column is editable
	public boolean isCellEditable(int i__rowIndex, int i__columnIndex)
	{
		if (i__columnIndex < 4)
			return false;
		else
			return true;
	}

	public Object getValueAt(int i__rowIndex, int i__columnIndex)
	{
		return data.get(i__rowIndex).get(i__columnIndex);
	}

	public void setValueAt(Object value, int i__rowIndex, int i__columnIndex)
	{
		data.get(i__rowIndex).setElementAt(value, i__columnIndex);
		fireTableCellUpdated(i__rowIndex,i__columnIndex);
	}
	
	public void insertData(Vector<Object> i__values)
	{
		data.add(i__values);
		fireTableDataChanged();
	 }
	 
	 public void removeRow(int i__rowIndex)
	 {
		data.removeElementAt(i__rowIndex);
		fireTableDataChanged();
	 }
	 
	 public void removeAll()
	 {
		 if (getRowCount() > 0)
		 { 
			 data.removeAllElements();
			 fireTableDataChanged();
		 }
	 }
 
    }
