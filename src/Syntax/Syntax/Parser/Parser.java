package Syntax.Syntax.Parser;

import Lexer.Lexer;
import Lexer.Token;
import Lexer.TokenType;
import Syntax.Parser.Tree.*;

import java.util.ArrayList;
import java.util.List;

public class Parser
{
	public Lexer Lexer;

	Token CurrentToken;

	private Utilities _utilities;

	public Parser(Lexer lexer)
	{
		Lexer = lexer;
		CurrentToken = lexer.GetNextToken();

		_utilities = new Utilities(this);
	}

	private Utilities getUtilities()
	{
		return _utilities;
	}

	public final List<StatementNode> Parse() throws Exception {
		 List<StatementNode> code  = CupCode();

			if (CurrentToken.getTokenType() != TokenType.EndOfFile)
			{
				throw new RuntimeException("End of file expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
			}
        return code;
    }

    private List<StatementNode> CupCode() throws Exception {
	    return	ListOfSentences();
    }

	private List<StatementNode> ListOfSentences() throws Exception {
		if (getUtilities().CompareTokenType(TokenType.EndOfFile))
		{
			return new ArrayList<>();
		}
		//Lista_Sentencias->Sentence Lista_Sentencias
		if (!getUtilities().CompareTokenType(TokenType.EndOfFile))
		{
			StatementNode statement = Sentence();
			List<StatementNode> statementList = ListOfSentences();

			statementList.add(0,statement);

			return statementList;
		}
		//Lista_Sentencia->Epsilon
		else
		{
            return new ArrayList<>();
		}
	}

	private StatementNode Sentence() throws Exception {
		System.out.println("\n");
	    if (getUtilities().CompareTokenType(TokenType.RW_PACKAGE)){
			getUtilities().NextToken();

            return package_spec();
        }
        else if (getUtilities().CompareTokenType(TokenType.RW_IMPORT)){
            getUtilities().NextToken();

            return import_spec();
        }
        else if(getUtilities().CompareTokenType(TokenType.RW_PARSER)
                || getUtilities().CompareTokenType(TokenType.RW_ACTION)
                || getUtilities().CompareTokenType(TokenType.RW_INIT)
                || getUtilities().CompareTokenType(TokenType.RW_SCAN)){

            TokenType type = CurrentToken.getTokenType();

            getUtilities().NextToken();

            return code_parts(type);
        }
        else if(getUtilities().CompareTokenType(TokenType.RW_TERMINAL)
                || getUtilities().CompareTokenType(TokenType.RW_NONTERMINAL)
                || getUtilities().CompareTokenType(TokenType.RW_NON)){

            TokenType type = CurrentToken.getTokenType();

            getUtilities().NextToken();

            return symbol_list(type);
        }
        else if (getUtilities().CompareTokenType(TokenType.RW_PRECEDENCE)){
            getUtilities().NextToken();

            return precedence_list();
        }
        else if(getUtilities().CompareTokenType(TokenType.RW_START)){
            getUtilities().NextToken();

            return start_spec();
        }
		else if (getUtilities().CompareTokenType(TokenType.Identifier))
		{
            //getUtilities().NextToken();
            return production_list();
		}
		else
		{
		    throw new Exception("Not a valid sentence at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
		}
    }

	//PRODUCTIONS DECLARATIONS
    private ProductionNode production_list() throws Exception {
      String nameOfNonterminal = CurrentToken.getLexeme();
	   getUtilities().NextToken();

	   ArrayList<Production> list = new ArrayList<>();
	   if (getUtilities().CompareTokenType(TokenType.OP_ASSIGNMENT)){
           getUtilities().NextToken();

           productions_list(list);

           if (getUtilities().CompareTokenType(TokenType.EndOfSentence)){
               getUtilities().NextToken();
           }
           else{
               throw  new Exception("End of sentence expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
           }
       }
       else
       {
           throw new Exception("Assignment operator ::= expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
       }

        ProductionNode productionNode = new ProductionNode();
        productionNode.nonTerminal = nameOfNonterminal;
        productionNode.productions = list;

        productionNode.Position = getPosition();

        return productionNode;
    }

    private void productions_list(ArrayList<Production> list) throws Exception {

        Production production = new Production();
        while (getUtilities().CompareTokenType(TokenType.Identifier)){

            production.production += " " + (CurrentToken.getLexeme());
            getUtilities().NextToken();

            if (getUtilities().CompareTokenType(TokenType.Colon)){

                production.production += (CurrentToken.getLexeme());
                getUtilities().NextToken();

                if (!getUtilities().CompareTokenType(TokenType.Identifier)){
                        throw  new Exception("A label was expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
                }
                production.production += (CurrentToken.getLexeme());
                getUtilities().NextToken();
            }

            if(getUtilities().CompareTokenType(TokenType.JavaCode)){
                production.production += " javaCode " ;
                production.javaCode = CurrentToken.getLexeme();
                getUtilities().NextToken();
            }

            if (getUtilities().CompareTokenType(TokenType.OP_PRECEDENCE)){
                production.production += " " + (CurrentToken.getLexeme());
                getUtilities().NextToken();

                if (!getUtilities().CompareTokenType(TokenType.Identifier)){
                    throw  new Exception("A label was expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
                }
                production.production += (CurrentToken.getLexeme());
                getUtilities().NextToken();
            }
        }

        list.add(production);

        if (getUtilities().CompareTokenType(TokenType.OP_PIPE))
        {
            optional_production(list);
        }
    }

    private void optional_production(ArrayList<Production> list) throws Exception {
        if (getUtilities().CompareTokenType(TokenType.OP_PIPE))
        {
            getUtilities().NextToken();
            productions_list(list);
        }
        else
        {

        }
    }


    //START WITH DECLARATION
    private StartNode start_spec() throws Exception {
        getUtilities().NextToken();

        String nonTerminal = null;
        if (getUtilities().CompareTokenType(TokenType.Identifier)){
            nonTerminal = CurrentToken.getLexeme();
            getUtilities().NextToken();

            if (getUtilities().CompareTokenType(TokenType.EndOfSentence)){
                getUtilities().NextToken();
            }
            else{
                throw  new Exception("End of sentence expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
            }
        }

        StartNode startNode = new StartNode();
        startNode.nonTerminal = nonTerminal;
        startNode.Position = getPosition();

        return startNode;
    }

    //PRECEDENCE DECLARATION
    private PrecedenceNode precedence_list() throws Exception {

        TokenType associativity;

        ArrayList<String> list;
        if (getUtilities().CompareTokenType(TokenType.RW_LEFT)
                || getUtilities().CompareTokenType(TokenType.RW_RIGHT) || getUtilities().CompareTokenType(TokenType.RW_NONASSOC)) {
            associativity = CurrentToken.getTokenType();
            getUtilities().NextToken();

            list = new ArrayList<>();
            symbols_list(list);

            if (getUtilities().CompareTokenType(TokenType.EndOfSentence)) {
                getUtilities().NextToken();
            } else {
                throw new Exception("End of sentence expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
            }
        } else {
            throw new Exception("An associativity is expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
        }

        PrecedenceNode precedenceNode = new PrecedenceNode();
        precedenceNode.associativity = associativity;
        precedenceNode.terminals = list;
        precedenceNode.Position = getPosition();

        return precedenceNode;

    }

    // SECTION OF SYMBOLS DECLARATION
    private SymbolDeclarationNode symbol_list(TokenType type) throws Exception {

	    TokenType classname = null;
        if (getUtilities().CompareTokenType(TokenType.RW_TERMINAL)){
            getUtilities().NextToken();
            type = TokenType.RW_NONTERMINAL;
        }

        //For datatypes before declaration Intenger... string etc
        if(getUtilities().CompareTokenType(TokenType.RW_OBJECT)
                || getUtilities().CompareTokenType(TokenType.RW_STRING)
                || getUtilities().CompareTokenType(TokenType.RW_INTEGER)
                || getUtilities().CompareTokenType(TokenType.RW_FLOAT)
                || getUtilities().CompareTokenType(TokenType.RW_DOUBLE)){

            classname = CurrentToken.getTokenType();
            getUtilities().NextToken();
        }

        ArrayList<String> list = new ArrayList<>();
        symbols_list(list);

        if (getUtilities().CompareTokenType(TokenType.EndOfSentence)){
            getUtilities().NextToken();
        }
        else{
            throw  new Exception("End of sentence expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
        }

        SymbolDeclarationNode symbolDeclarationNode = new SymbolDeclarationNode();

        symbolDeclarationNode.TypeOfSymbol = type;
        symbolDeclarationNode.ClassName = classname;
        symbolDeclarationNode.symbolNames = list;
        symbolDeclarationNode.Position = getPosition();

        return symbolDeclarationNode;
    }

    private void symbols_list(ArrayList<String> list) throws Exception {

        if (getUtilities().CompareTokenType(TokenType.Identifier)){
            list.add(CurrentToken.getLexeme());
            getUtilities().NextToken();

            optional_symbol(list);
        }
        else{
            throw  new Exception("Identifier expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
        }
    }

    private void optional_symbol(ArrayList<String> list) throws Exception {
        if (getUtilities().CompareTokenType(TokenType.Comma))
        {
            getUtilities().NextToken();
            symbols_list(list);
        }
        else
        {

        }
    }

    //SECTION OF CODE PARTS
    private CodePartNode code_parts(TokenType type) throws Exception {

        getUtilities().NextToken();
        String code = code_part();

        if (getUtilities().CompareTokenType(TokenType.EndOfSentence)){
            getUtilities().NextToken();
        }
        else{
            throw  new Exception("End of sentence expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
        }

        CodePartNode codePartNode = new CodePartNode();

        codePartNode.CodePartType = type;
        codePartNode.JavaCode = code;
        codePartNode.Position = getPosition();

        return codePartNode;
    }

    private String code_part() throws Exception {
        String code;
	    if (getUtilities().CompareTokenType(TokenType.JavaCode)){

            code = CurrentToken.getLexeme();
            getUtilities().NextToken();
        }
        else
        {
            throw new Exception("Java Code token expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
        }

        return code;
    }


    //SECTION OF IMPORTS
    private ImportNode import_spec() throws Exception {

        ArrayList<String> list = new ArrayList<>();
	    import_id(list);

        if (getUtilities().CompareTokenType(TokenType.EndOfSentence)){
            getUtilities().NextToken();
        }
        else{
            throw  new Exception("End of sentence expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
        }

        ImportNode importNode = new ImportNode();

        importNode.packages = list;
        importNode.Position = getPosition();

        return  importNode;
    }

    private void import_id(ArrayList<String> list) throws Exception {

        if (getUtilities().CompareTokenType(TokenType.OP_INCLUDEALL) ){
            list.add(CurrentToken.getLexeme());
            getUtilities().NextToken();
            return;
        }
	    if (getUtilities().CompareTokenType(TokenType.Identifier) ){
            list.add(CurrentToken.getLexeme());
            getUtilities().NextToken();

            optional_import_id(list);
        }
        else{
            throw  new Exception("Identifier expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
        }
    }

	private void optional_import_id(ArrayList<String> list) throws Exception {
		if (getUtilities().CompareTokenType(TokenType.Dot))
		{
			getUtilities().NextToken();
			import_id(list);
		}
		else
		{

		}
	}


	//PACKAGE IMPORT SECTION
    private PackageNode package_spec() throws Exception {

	    ArrayList<String> list = new ArrayList<>();

	    package_id(list);

        if (getUtilities().CompareTokenType(TokenType.EndOfSentence)){
            getUtilities().NextToken();
        }
        else{
            throw  new Exception("End of sentence expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
        }

        PackageNode  packageNode = new PackageNode();

        packageNode.packages = list;
        packageNode.Position = getPosition();

        return packageNode;
    }

    private void package_id(ArrayList<String> list) throws Exception {

	    if (getUtilities().CompareTokenType(TokenType.Identifier)){
            list.add(CurrentToken.getLexeme());
	        getUtilities().NextToken();

            optional_package_id(list);
        }
        else{
            throw  new Exception("Identifier expected at row: " + CurrentToken.getRow() + " , column: " + CurrentToken.getColumn());
        }
    }

    private void optional_package_id(ArrayList<String> list) throws Exception {
        if (getUtilities().CompareTokenType(TokenType.Dot))
        {
            getUtilities().NextToken();
            package_id(list);
        }
        else
        {

        }
    }


    private Token getPosition() {
        Token position = new Token();
        position.setRow(CurrentToken.getRow());
        position.setColumn(CurrentToken.getColumn());
        return position;
    }
}