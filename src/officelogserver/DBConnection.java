package officelogserver;

/**
 * Defines the parameters needed to connect to the database
 * 
 * @author Zooty
 */
public interface DBConnection {
    String DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";    
    String URLLOCAL = "jdbc:sqlserver://localhost:1433;databaseName=OfficelogDB;";
    String URLREMOTE = "jdbc:sqlserver://zoliftp.dlinkddns.com:1433;databaseName=OfficelogDB;";
    String URL = URLLOCAL;
    String USER = "officelogUser";
    String PASSW = "officelogPW";
    
    //----------------------------------------------------------------------------------------------
    
    String SQLSELECTROOMS1 = "SELECT * FROM Rooms";
    String SQLSELECTROOMS2 = "SELECT Name FROM Rooms";
    String SQLSELECTROOMCONNECTIONS = "SELECT * FROM RoomConnections";
    String SQLSELECTPERMISSIONS = "SELECT * FROM Permissions";
    String SQLSELECTPEOPLE1 = "SELECT ID, Name, Loc, Pic, Job FROM People WHERE IsDeleted = 0";
    String SQLSELECTPEOPLE2 = "SELECT Date, Type, ID, Name, l.Loc "
                            + "FROM Logs l, People p WHERE l.PersonID = p.ID\n";
    String SQLSELECTLOGSPEOPLE1 = "SELECT Name, ID FROM Logs l, People p "
                                + "WHERE l.PersonID = p.ID GROUP BY Name, ID";
    String SQLSELECTLOGSPEOPLE2FIRST = "SELECT TOP ";
    String SQLSELECTLOGSPEOPLE2SECOND = " ID, Name, count(*) Attempts\n"
                                        + "FROM Logs l, People p\n"
                                        + "WHERE l.PersonID = p.ID AND l.Type = 'Access Denied'\n"
                                        + "Group by ID, Name\n"
                                        + "ORDER BY Attempts ";
    String SQLSELECTLOGSPEOPLE3 = "SELECT Date, ID, Name, logs.Loc\n"
                                + "FROM logs, People\n"
                                + "WHERE ID = PersonID AND Type = 'Entered'";
    
    String SQLINSERTPERMISSIONS1 = "INSERT INTO Permissions VALUES(";
    String SQLINSERTPERMISSIONS2 = "INSERT INTO Permissions VALUES(?,?)";
    String SQLINSERTPEOPLE1 = "INSERT INTO People VALUES(?,?,?,?,?,?)";
    String SQLINSERTLOGS = "INSERT INTO Logs VALUES('Access Denied', CURRENT_TIMESTAMP,";
    
    String SQLUPDATEPEOPLE1 = "UPDATE People SET Pic = ?, Job = ? WHERE ID = ?";
    String SQLUPDATEPEOPLE2 = "UPDATE People SET IsDeleted = 1 Where ID = ";
    String SQLUPDATEPEOPLE3FIRST = "UPDATE People SET Loc = '";
    String SQLUPDATEPEOPLE3SECOND = " WHERE ID = ";
    
    String SQLDELETEPERMISSIONS = "DELETE FROM Permissions WHERE PersonID = ";
}
