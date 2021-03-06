package anno;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

import anno.utils.AnnoUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

//import noa.PGen;

@SupportedAnnotationTypes(value = { "anno.PP" })
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class PPProcessor extends AbstractProcessor {
	public static final String TAB = "\t";
	public static final String TAB2 = "\t\t";
	public static final String TAB3 = "\t\t\t";
	public static final String TAB4 = "\t\t\t\t";

	private Filer filer;

	@Override
	public void init(ProcessingEnvironment env) {
		filer = env.getFiler();
	}

	private String[] toList(String message) {
		return message.split(",");
	}


	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment env) {

		String folder = "ppgen";
		
		String printerRes = "";
		
		// Add the factory interface PP here.
		printerRes += "package ppgen;\n";
		printerRes += "public interface IPrint {\n";
		printerRes += TAB + "void print();\n";
		printerRes += "}\n";
		
		try {
			// Also create the public interface Printer.
			JavaFileObject printerFile;
			printerFile = filer.createSourceFile(folder + "/IPrint", null);
			printerFile.openWriter().append(printerRes).close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Collect all the interfaces with PP
		for (Element element : env.getElementsAnnotatedWith(PP.class)) {
			// Initialization.
			TypeMirror tm = element.asType();
			String typeArgs = tm.accept(new DeclaredTypeVisitor(), element);
			String[] lTypeArgs = toList(typeArgs);

			String name = element.getSimpleName().toString();
			String res = createPPClass(folder, (TypeElement) element,
					lTypeArgs, typeArgs);

			try {
				JavaFileObject jfo;
				jfo = filer.createSourceFile(folder + "/" + nameGenPP(name),
						element);
				jfo.openWriter().append(res).close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	private String nameGenPP(String name) {
		return "PP" + name;
	}

	private String getName(Element e) {
		return e.getSimpleName().toString();
	}

	private int getNumOfTypeParams(TypeElement e) {
		return e.getTypeParameters().size();
	}

	private String produceClassHeader(int numOfParams) {
		String s = "<IPrint";
		// Should iterate for numOfParams - 1 times.
		for (int count = 1; count < numOfParams; count++) {
			s += ", IPrint";
		}
		s += ">";
		return s;
	}
	
	private String createPPClass(String folder, TypeElement te,
			String[] lTypeArgs, String typeArgs) {
		String name = getName(te);
		int numOfTypeParams = getNumOfTypeParams(te);
		String res = "package " + folder + ";\n\n";
		res += "import " + getPackage(te) + "." + name + ";\n\n";
		res += "import " + "de.uka.ilkd.pp.*;\n";
		res += "import " + "java.io.*;\n";
		
		
		res += "public class " + nameGenPP(name) + " implements " + name
				+ produceClassHeader(numOfTypeParams) + " {\n";

		// Here we'll set some class-level variables.
		// Let's put it as 20 now for testing purpose.
		res += TAB + "public static final int DEFAULT_LINE_WIDTH = 20;\n";
		res += TAB + "public static final int DEFAULT_INDENTATION = 2;\n\n";
		
		res += TAB + "public StringBackend back = new StringBackend(DEFAULT_LINE_WIDTH);\n";
		res += TAB + "Layouter<NoExceptions> pp = new Layouter<NoExceptions>(back, DEFAULT_INDENTATION);\n";
		
		// For each data type that we know to exist in the target language,
		// we'll generate the appropriate printing method. The actual generation
		// is done in the method "genPrintMethod"
		List<? extends Element> le = te.getEnclosedElements();
		for (Element e : le) {
			String methodName = e.getSimpleName().toString();
			String[] args = { methodName, typeArgs, name };
			// res += e.asType().accept(new PrintMethodVisitor(), args);
			res += genPrintMethod((ExecutableElement) e, typeArgs);
		}

		res += "}";
		return res;
	}

	private String genPrintMethod(ExecutableElement e, String typeArgs) {
		String[] lTypeArgs = typeArgs.split(",");
		String[] lListTypeArgs = new String[lTypeArgs.length];

		for (int listTypeArgsCount = 0; listTypeArgsCount < lTypeArgs.length; ++listTypeArgsCount) {
			lListTypeArgs[listTypeArgsCount] = "java.util.List<"
					+ lTypeArgs[listTypeArgsCount] + ">";
		}

		String res = "";
		res += TAB + "public IPrint " + e.getSimpleName() + "(";
		List<? extends VariableElement> params = e.getParameters();
		// Determine the correct Java type of the parameter to be fed into this
		// printing method
		for (int tempParamCount = 0; tempParamCount < params.size(); ++tempParamCount) {
			if (AnnoUtils.arrayContains(lListTypeArgs, params.get(tempParamCount).asType()
					.toString()) != -1) {
				res += "java.util.List<IPrint> p" + tempParamCount;
			} else if (AnnoUtils.arrayContains(lTypeArgs, params.get(tempParamCount)
					.asType().toString()) != -1) {
				res += "IPrint p" + tempParamCount;
			} else {
				// Have to add break between parameters otherwise they'll all be
				// crammed together.
				res += params.get(tempParamCount).asType().toString() + " p" + tempParamCount;
			}
			if (tempParamCount < params.size() - 1)
				res += ", ";
		}
		res += ") {\n";
		// This was the beginning of returning a string. Now we'll change it a bit.
		// Currently for each expression we're creating a local layout object to ensure the format.
		// Actually let's not do it. We can still put the thing as a global object.
		
		res += TAB2 + "return () -> {\n";
		res += TAB3 + "pp.beginI();\n";

		// We already defined an annotation in our framework called "Syntax" for
		// each language. We're just extracting that information.
		String syn = e.getAnnotation(Syntax.class).value();
		String[] synList = syn.split(" ");

		// This i is used to record which parameter we're currently trying to
		// print. (ird)
		// The "synList" is to say we separate different symbols in the one line
		// of notation. synListCount is to record which one among the list we're currently
		// printing.

		// It seems that we start from synListCount = 2 bceause the first two things are
		// `form =`, mandatory components of the annotation.
		int paramCount = 0, synListCount = 2;
		while (synListCount < synList.length) {
			// If the symbol starts with ' then this symbols is a keyword.
			while (synListCount < synList.length && synList[synListCount].startsWith("\'")) {
				// substring(1, length() - 1) is to get rid of the ' ' at both
				// ends.
				String currentSyn = synList[synListCount].substring(1,
						synList[synListCount].length() - 1);
				
				res += TAB3 + "pp.print(" + "\"" + currentSyn + "\");\n";
				// Note a space is added after the keyword, if synListCount is not a
				// starting parentheses or the last symbol.
				if (!(currentSyn.contains("(") || synListCount > synList.length - 2)) {
					res += TAB3 + "pp.brk();\n";
				} 	
				synListCount++;
			}
			// It seems that the additional check is because synListCount could also be
			// incremented inside of the while loop itself. (synListCount++)
			if (synListCount < synList.length) {
				String paramName = "p" + paramCount;
				String str = synList[synListCount];
				// So "@" indicates the place where separators are to appear
				// following it.
				if (str.contains("@")) {
					String separator = getSeparator(synList[synListCount]);
					// Here the arrayOutOfBounds error is thrown for function
					// invocation of Mumbler.
					if (AnnoUtils.arrayContains(lListTypeArgs, params.get(paramCount)
							.asType().toString()) != -1) {
						// In this case the argument itself is a list of printers.
						res += TAB3 + "for (int count = 0; count < " + paramName + ".size() - 1; count++) {\n";
						res += TAB4 + paramName + ".get(count).print();\n";
						res += TAB4 + "pp.print(\"" + separator + "\");\n";
						res += TAB4 + "pp.brk();\n";
						res += TAB3 + "}\n";
						// Print the last element of the list without printing extra breaks.
						res += TAB3 + paramName + ".get(" + paramName + ".size() - 1).print();\n";
					} else {
						// TODO: error: list type does not match!
						// res += "Error here. List type mismatch occurence 1.";
					}
				}

				if (AnnoUtils.arrayContains(lListTypeArgs, params.get(paramCount)
						.asType().toString()) != -1) {
					// TODO: error: list type does not match!
					// res += "Error here. List type mismatch occurence 2.";
				} else if (AnnoUtils.arrayContains(lTypeArgs, params.get(paramCount)
						.asType().toString()) != -1) {
					// In this case it's just one single printer argument, not a list.
					res += TAB3 + paramName + ".print();\n";
					// Have to add space between parameters otherwise they'll
					// all be crammed together.
					// We add a break unless the param is the last one or is
					// followed by )
					if (!(synListCount == synList.length - 1)
							&& !(synList[synListCount + 1].contains(")"))) {
						res += TAB3 + "pp.brk();\n";
					}
				} else { // int, bool, float....
					// In this case it's a primitive type. We should just
					// directly print its literal representation.
					// The \"\" here is just a hack to force the param to be
					// displayed as String without having to call `toString`...
					String temp = "\"\" + " + paramName;
					res += TAB3 + "pp.print(" + temp + ");\n";

					// We add a space unless the literal is the last one or is
					// followed by )
					if (!(synListCount == synList.length - 1)
							&& !(synList[synListCount + 1].contains(")"))) {
						res += TAB3 + "pp.brk();\n";
					}
				}
        // Preventative for arrayOutOfBounds error.
				if (paramCount < params.size() - 1) {
					paramCount++;
				}
				synListCount++;
			}
		}

		res += "\n";
		res += TAB3 + "pp.end();\n";
		res += TAB2 + "};\n";
		res += TAB + "}\n";

		/* print debugging info. */
		res += "/* \n";
		res += "params.size(): " + params.size() + "\n";
		res += "Original syn: " + syn + "\n";
		res += "Original synList: " + Arrays.toString(synList) + "\n";
		res += "synList.length: " + synList.length + "\n";
		res += "e.getParameters(): " + e.getParameters() + "\n";
		for (VariableElement param : params) {
			res += param.toString() + ": " + param.asType() + "\n";
		}
		res += "typeArgs: " + typeArgs + "\n";
		res += "lListTypeArgs: ";
		for (String t : lListTypeArgs) {
			res += t + ", ";
		}
		res += "\n";
		res += e.getAnnotation(Syntax.class).value() + "\n";
		res += "\n */ \n\n";
		return res;
	}

	private String getSeparator(String str) {
		// getSeparator( "exp@','+" ) ---> ","
		int i = str.indexOf("@");
		// Note that if the thing is of form '', then it uses space as separator by default and we shouldn't add anything extra.
		if (str.substring(i+1, i+3).equals("''")) {
			return "";
		} else {
			return str.substring(i+2, i+3);
		}
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	private String getPackage(Element element) {
		return ((PackageElement) element.getEnclosingElement())
				.getQualifiedName().toString();
	}

}
