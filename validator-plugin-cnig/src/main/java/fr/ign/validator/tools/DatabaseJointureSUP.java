package fr.ign.validator.tools;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Utilitaire permettant de parcourir les liens dans les tables de SUP
 * 
 * @author MBorne
 *
 */
public class DatabaseJointureSUP {

	/**
	 * La connexion à la base de données
	 */
	private Connection connection;
	

	/**
	 * Construction de la base de données avec un chemin
	 * 
	 * @param parentDirectory
	 * @throws SQLException
	 */
	public DatabaseJointureSUP(File parentDirectory) throws SQLException {
		try {
			Class.forName("org.sqlite.JDBC");

			File databasePath = new File(parentDirectory, "jointure_sup.db");
			//File databasePath = parentDirectory ;
			String databaseUrl = "jdbc:sqlite:"+ databasePath.getAbsolutePath() ;
			connection = DriverManager.getConnection(databaseUrl);
			connection.setAutoCommit(false);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		createSchema();
	}

	/**
	 * Création du schéma
	 * 
	 * @throws SQLException
	 */
	private void createSchema() throws SQLException {
		createTableActe();
		createTableServitude();
		createTableGenerateur();
		createTableAssiete();
	}

	private void createTableActe() throws SQLException {
		Statement sth = connection.createStatement();
		sth.executeUpdate(
			"  CREATE TABLE acte ("
			+ " id TEXT PRIMARY KEY, "
			+ " fichier TEXT"
			+ ")"
		);
		connection.commit(); 
	}

	private void createTableServitude() throws SQLException {
		Statement sth = connection.createStatement();
		sth.executeUpdate(
			"  CREATE TABLE servitude ("
			+ "  id TEXT, " // PRIMARY KEY : impossible, pas unique
			+ "  id_acte TEXT REFERENCES acte(id) "
			+ ")"
		);
		connection.commit(); 
	}

	private void createTableGenerateur() throws SQLException {
		Statement sth = connection.createStatement();
		sth.executeUpdate(
			"  CREATE TABLE generateur ("
			+ "  id TEXT PRIMARY KEY, "
			+ "  id_servitude TEXT REFERENCES servitude(id)"
			+ ")"
		);
		connection.commit(); 
	}

	private void createTableAssiete() throws SQLException {
		Statement sth = connection.createStatement();
		sth.executeUpdate(
			"  CREATE TABLE assiette ("
			+ "  id TEXT PRIMARY KEY, "
			+ "  id_generateur TEXT REFERENCES generateur(id)"
			+ ")"
		);
		connection.commit(); 
	}
	
	
	/**
	 * Renvoie le nombre d'acte
	 * @return
	 * @throws SQLException
	 */
	public int getCountActes() throws SQLException {
		return getCount("acte");
	}
	
	/**
	 * Chargement du fichier des actes
	 * @param path
	 * @throws IOException 
	 * @throws SQLException 
	 */
	public void loadFileActe(File actesFile) throws IOException, SQLException {
		TableReader reader = new TableReader(actesFile);
		
		int indexIdActe  = reader.findColumn("IDACTE");
		if ( indexIdActe < 0 ){
			throw new IOException("Colonne IDACTE non trouvée");
		}
		
		int indexFichier = reader.findColumn("FICHIER");
		if ( indexFichier < 0 ){
			throw new IOException("Colonne FICHIER non trouvée");
		}
		
		PreparedStatement sth = connection.prepareStatement("INSERT INTO acte (id,fichier) VALUES (?,?)" );
		
		while ( reader.hasNext() ){
			String[] row = reader.next() ;
			
			String idActe  = row[indexIdActe] ;
			String fichier = row[indexFichier] ;
			
			sth.setString(1, idActe);
			sth.setString(2, fichier);
			
			sth.addBatch();
		}
		sth.executeBatch();		
		connection.commit();
	}
	
	
	/**
	 * Renvoie le nombre d'acte
	 * @return
	 * @throws SQLException
	 */
	public int getCountServitude() throws SQLException {
		return getCount("servitude");
	}
	

	/**
	 * Chargement du fichier des servitudes
	 * @param path
	 * @throws IOException 
	 * @throws SQLException 
	 */
	public void loadFileServitude(File servitudesFile) throws IOException, SQLException {
		TableReader reader = new TableReader(servitudesFile);
		
		int indexIdSup  = reader.findColumn("idSup");
		if ( indexIdSup < 0 ){
			throw new IOException("Colonne IDACTE non trouvée");
		}
		
		int indexIdActe = reader.findColumn("idActe");
		if ( indexIdActe < 0 ){
			throw new IOException("Colonne FICHIER non trouvée");
		}
		
		
		PreparedStatement sth = connection.prepareStatement("INSERT INTO servitude (id,id_acte) VALUES (?,?)" );
		
		while ( reader.hasNext() ){
			String[] row = reader.next() ;
			
			String idSup  = row[indexIdSup] ;
			String idActe = row[indexIdActe] ;
			
			sth.setString(1, idSup);
			sth.setString(2, idActe);
			
			sth.addBatch();
		}
		sth.executeBatch();
		connection.commit();
	}
	
	/**
	 * Renvoie le nombre de générateurs
	 * @return
	 * @throws SQLException
	 */
	public int getCountGenerateur() throws SQLException {
		return getCount("generateur");
	}
	

	/**
	 * Chargement du fichier des générateurs
	 * @param path
	 * @throws IOException 
	 * @throws SQLException 
	 */
	public void loadFileGenerateur(File generateursFile) throws IOException, SQLException {
		TableReader reader = new TableReader(generateursFile);
		
		int indexIdGen  = reader.findColumn("idGen");
		if ( indexIdGen < 0 ){
			throw new IOException("Colonne IDGEN non trouvée");
		}
		
		int indexIdSup = reader.findColumn("idSup");
		if ( indexIdSup < 0 ){
			throw new IOException("Colonne FICHIER non trouvée");
		}
		
		
		PreparedStatement sth = connection.prepareStatement("INSERT INTO generateur (id,id_servitude) VALUES (?,?)" );
		
		while ( reader.hasNext() ){
			String[] row = reader.next() ;
			
			String idGen = row[indexIdGen] ;
			String idSup = row[indexIdSup] ;
			
			sth.setString(1, idGen);
			sth.setString(2, idSup);
			
			sth.addBatch();
		}
		sth.executeBatch();
		connection.commit();
	}
	

	/**
	 * Chargement du fichier des assiettes
	 * @param path
	 * @throws IOException 
	 * @throws SQLException 
	 */
	public void loadFileAssiette(File assiettesFile) throws IOException, SQLException {
		TableReader reader = new TableReader(assiettesFile);
		
		int indexIdAss  = reader.findColumn("IDASS");
		if ( indexIdAss < 0 ){
			throw new IOException("Colonne IDASS non trouvée");
		}
		
		int indexIdGen = reader.findColumn("IDGEN");
		if ( indexIdGen < 0 ){
			throw new IOException("Colonne IDGEN non trouvée");
		}
		
		PreparedStatement sth = connection.prepareStatement("INSERT INTO assiette (id,id_generateur) VALUES (?,?)" );
		
		while ( reader.hasNext() ){
			String[] row = reader.next() ;
			
			String idAss = row[indexIdAss] ;
			String idGen = row[indexIdGen] ;
			
			sth.setString(1, idAss);
			sth.setString(2, idGen);
			
			sth.addBatch();
		}
		sth.executeBatch();
		connection.commit();
	}

	/**
	 * Renvoie le nombre des assiettes	
	 * @return
	 * @throws SQLException 
	 */
	public Object getCountAssiette() throws SQLException {
		return getCount("assiette");
	}

	
	/**
	 * Recherche des actes pour un generateur
	 * @param idGen
	 * @return 
	 * @throws SQLException
	 */
	public List<String> findFichiersByGenerateur(String idGen) {
		String sql = "SELECT * FROM acte "
				+ " LEFT JOIN servitude ON acte.id = servitude.id_acte "
				+ " LEFT JOIN generateur ON generateur.id_servitude = servitude.id "
				+ " WHERE generateur.id = ?"
		;
		try {
			PreparedStatement sth = connection.prepareStatement(sql);
			sth.setString(1, idGen);
			return getFichiersFromResultSet(sth.executeQuery()) ;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Recherche des actes pour un generateur
	 * @param idGen
	 * @return 
	 * @throws SQLException
	 */
	public List<String> findFichiersByAssiette(String idAss) {
		String sql = "SELECT * FROM acte "
				+ " LEFT JOIN servitude ON acte.id = servitude.id_acte "
				+ " LEFT JOIN generateur ON generateur.id_servitude = servitude.id "
				+ " LEFT JOIN assiette ON assiette.id_generateur = generateur.id "
				+ " WHERE assiette.id = ?"
		;
		try {
			PreparedStatement sth = connection.prepareStatement(sql);
			sth.setString(1, idAss);
			return getFichiersFromResultSet(sth.executeQuery()) ;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException 
	 */
	private List<String> getFichiersFromResultSet(ResultSet rs) throws SQLException{
		List<String> result = new ArrayList<String>() ;
		while (rs.next()) {
			result.add( rs.getString("fichier") );
        }
		return result ;
	}
	
	
	/**
	 * Effectue un comptage dans une table 
	 * @param tableName
	 * @return
	 * @throws SQLException
	 */
	private int getCount(String tableName) throws SQLException{
		PreparedStatement sth = connection.prepareStatement("SELECT count(*) FROM "+tableName) ;
		ResultSet rs = sth.executeQuery() ;
		return rs.getInt(1);
	}

	
	
}
