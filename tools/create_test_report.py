"""Genera el informe únicamente a partir de resultados y capturas existentes."""
from __future__ import annotations

import argparse
import json
from datetime import datetime, timedelta, timezone
from pathlib import Path
from xml.etree import ElementTree as ET
from xml.sax.saxutils import escape

from PIL import Image as PILImage
from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate, Frame, Image, KeepTogether, PageBreak, PageTemplate,
    Paragraph, Spacer, Table, TableStyle,
)

ROOT = Path(__file__).resolve().parents[1]
PROOFS = ROOT / "output" / "pruebas"
TARGET = ROOT / "output" / "pdf" / "Pruebas_Entre_Paginas.pdf"
REPO = "https://github.com/Anderson092205/Desafio3_DSM"
GREEN = colors.HexColor("#25675C")
INK = colors.HexColor("#26312D")
MUTED = colors.HexColor("#67726B")
CREAM = colors.HexColor("#F7F6F2")
PALE = colors.HexColor("#E5EEE5")
GOLD = colors.HexColor("#BF8B42")
PAGE_W, PAGE_H = A4
MARGIN = 42
WIDTH = PAGE_W - 2 * MARGIN

CAPTIONS = {
    "00": ("Inicio de sesión", "Pantalla inicial de acceso a la biblioteca personal."),
    "01": ("Validación de acceso", "El correo incorrecto y una contraseña de menos de seis caracteres muestran errores antes de enviar datos a Firebase."),
    "02": ("Confirmación de contraseña", "El registro se bloquea cuando la confirmación no coincide con la contraseña."),
    "03": ("Registro completado", "La nueva cuenta accede a su inicio y puede definir su propia meta de lectura."),
    "04": ("Meta inválida", "Una meta de cero libros se rechaza y el mensaje explica que debe ser un entero mayor que cero."),
    "05": ("Meta anual guardada", "La meta de cinco libros queda guardada en Realtime Database para la cuenta autenticada."),
    "06": ("Validación de libro", "El formulario identifica título, autor y total de páginas inválidos."),
    "07": ("Biblioteca y progreso", "El principito aparece como Leyendo: 50 de 200 páginas equivalen al 25% de avance."),
    "08": ("Lectura en el inicio", "La lectura en curso se muestra junto a la meta anual y su porcentaje de avance."),
    "09": ("Calificación obligatoria", "Un libro terminado requiere una calificación de una a cinco estrellas."),
    "10": ("Finalización de lectura", "El formulario permite asignar cinco estrellas al libro terminado."),
    "11": ("Libro terminado", "La biblioteca muestra el estado Terminado, 100% de avance, cinco estrellas y la fecha de finalización."),
    "12": ("Estadísticas anuales", "El año actual contiene un libro terminado y 200 páginas; el conteo por género identifica Novela."),
    "13": ("Libro mejor calificado", "Las estadísticas identifican la lectura mejor calificada. En caso de empate se toma la fecha de finalización más temprana."),
    "14": ("Avance de la meta", "El inicio refleja un libro de cinco terminados y cuatro libros pendientes para cumplir la meta."),
    "15": ("Sesión cerrada", "Después de cerrar sesión se vuelve a la pantalla de acceso."),
    "16": ("Otra cuenta, otra biblioteca", "La segunda cuenta tiene una biblioteca vacía. La prueba también consulta el servidor y comprueba que no puede leer los datos de la primera."),
    "17": ("Datos recuperados", "Al iniciar sesión nuevamente con la primera cuenta, se recupera el libro terminado y su calificación."),
    "18": ("Sesión tras cerrar el proceso", "Después de cerrar forzadamente el proceso y volver a abrir la aplicación, la cuenta continúa autenticada. Esta prueba no reinicia el dispositivo."),
    "19": ("Datos tras reabrir la app", "La biblioteca y la meta se recuperan después del cierre forzado del proceso y la reapertura. Los resultados adicionales detallan lo verificado."),
}


def load_json(path: Path, required: bool = True):
    if not path.exists():
        if required:
            raise FileNotFoundError(f"Falta evidencia requerida: {path}")
        return None
    return json.loads(path.read_text(encoding="utf-8-sig"))


def register_fonts():
    candidates = [
        (Path("C:/Windows/Fonts/arial.ttf"), Path("C:/Windows/Fonts/arialbd.ttf")),
        (Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"), Path("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")),
    ]
    for regular, bold in candidates:
        if regular.exists() and bold.exists():
            pdfmetrics.registerFont(TTFont("Reading", str(regular)))
            pdfmetrics.registerFont(TTFont("Reading-Bold", str(bold)))
            pdfmetrics.registerFontFamily("Reading", normal="Reading", bold="Reading-Bold")
            return "Reading", "Reading-Bold"
    return "Helvetica", "Helvetica-Bold"


FONT, BOLD = register_fonts()
styles = getSampleStyleSheet()
styles.add(ParagraphStyle("ReadingTitle", fontName=BOLD, fontSize=30, leading=35, textColor=GREEN, spaceAfter=15))
styles.add(ParagraphStyle("ReadingH1", fontName=BOLD, fontSize=20, leading=25, textColor=GREEN, spaceAfter=14, keepWithNext=True))
styles.add(ParagraphStyle("ReadingH2", fontName=BOLD, fontSize=12, leading=16, textColor=INK, spaceAfter=7, keepWithNext=True))
styles.add(ParagraphStyle("ReadingBody", fontName=FONT, fontSize=10, leading=15, textColor=INK, spaceAfter=10))
styles.add(ParagraphStyle("ReadingSmall", fontName=FONT, fontSize=8.4, leading=12, textColor=MUTED, spaceAfter=6))
styles.add(ParagraphStyle("ReadingCaption", fontName=FONT, fontSize=9, leading=13, textColor=INK, spaceAfter=7))
styles.add(ParagraphStyle("ReadingCell", fontName=FONT, fontSize=8, leading=11, textColor=INK))
styles.add(ParagraphStyle("ReadingCellBold", fontName=BOLD, fontSize=8, leading=11, textColor=GREEN))
styles.add(ParagraphStyle("ReadingCenter", fontName=FONT, fontSize=9, leading=13, textColor=MUTED, alignment=TA_CENTER))


def p(text, style="ReadingBody"):
    return Paragraph(escape(str(text)).replace("\n", "<br/>"), styles[style])


def read_unit_results():
    paths = sorted((ROOT / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
    suites, cases = [], []
    for path in paths:
        suite = ET.parse(path).getroot()
        suites.append({key: int(suite.attrib.get(key, 0)) for key in ("tests", "failures", "errors", "skipped")})
        for case in suite.findall("testcase"):
            cases.append({"name": case.attrib.get("name", ""), "class": case.attrib.get("classname", ""),
                          "passed": case.find("failure") is None and case.find("error") is None and case.find("skipped") is None})
    return {key: sum(s[key] for s in suites) for key in ("tests", "failures", "errors", "skipped")}, cases


def table(rows, widths, header=True):
    content = [[p(value, "ReadingCellBold" if header and i == 0 else "ReadingCell") for value in row] for i, row in enumerate(rows)]
    result = Table(content, colWidths=widths, repeatRows=1 if header else 0, hAlign="LEFT")
    commands = [("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 9),
                ("RIGHTPADDING", (0, 0), (-1, -1), 9), ("TOPPADDING", (0, 0), (-1, -1), 7),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 7), ("LINEBELOW", (0, 0), (-1, -1), .4, colors.HexColor("#DDE3DA"))]
    if header:
        commands.append(("BACKGROUND", (0, 0), (-1, 0), PALE))
    result.setStyle(TableStyle(commands))
    return result


def background(canvas, doc):
    canvas.saveState()
    canvas.setFillColor(CREAM); canvas.rect(0, 0, PAGE_W, PAGE_H, fill=1, stroke=0)
    canvas.setFillColor(GREEN); canvas.rect(0, PAGE_H - 9, PAGE_W, 9, fill=1, stroke=0)
    canvas.setFont(BOLD, 9); canvas.drawString(MARGIN, PAGE_H - 31, "ENTRE PÁGINAS")
    canvas.setFillColor(MUTED); canvas.setFont(FONT, 8)
    canvas.drawRightString(PAGE_W - MARGIN, PAGE_H - 31, "Desafío práctico 3 - DSM441")
    canvas.setStrokeColor(colors.HexColor("#DDE3DA")); canvas.line(MARGIN, 38, PAGE_W - MARGIN, 38)
    canvas.drawString(MARGIN, 25, "Evidencias de funcionamiento | 24 de octubre de 2026")
    canvas.drawRightString(PAGE_W - MARGIN, 25, f"Página {doc.page}")
    canvas.restoreState()


def capture_panel(path: Path, number: int):
    prefix = path.stem.split("_", 1)[0]
    title, description = CAPTIONS.get(prefix, (path.stem.replace("_", " "), "Captura adicional de la ejecución real de la aplicación."))
    with PILImage.open(path) as img:
        w, h = img.size
    max_w, max_h = (WIDTH - 26) / 2, 470
    scale = min(max_w / w, max_h / h)
    image = Image(str(path), width=w * scale, height=h * scale)
    image.hAlign = "CENTER"
    return [p(f"{number:02d}  {title}", "ReadingH2"), image, Spacer(1, 10), p(description, "ReadingCaption"),
            p(path.name, "ReadingSmall")]


def create_report(allow_incomplete=False):
    backend = load_json(PROOFS / "firebase-results.json")
    device = load_json(PROOFS / "dispositivo.json", required=False) or {}
    build = load_json(PROOFS / "compilacion.json", required=False) or {}
    ui_path = PROOFS / "resultado_pruebas.json"
    if not ui_path.exists():
        ui_path = PROOFS / "capturas" / "resultado_pruebas.json"
    ui = load_json(ui_path, required=not allow_incomplete) or {}
    unit, cases = read_unit_results()
    backend_passed = sum(result.get("passed") is True for result in backend.get("results", []))
    backend_total = len(backend.get("results", []))
    ui_passed = ui.get("resultado") == "APROBADO"
    unit_passed = unit["tests"] > 0 and unit["failures"] == unit["errors"] == 0
    capture_dir = PROOFS / "capturas"
    selected_names = ui.get("capturas", [])
    captures = [capture_dir / name for name in selected_names if name != "99_estado_al_fallar.png"]
    if not captures:
        captures = sorted(capture_dir.glob("*.png"))
    else:
        captures.extend(path for path in sorted(capture_dir.glob("*.png"))
                        if path not in captures and path.stem.split("_", 1)[0] in {"18", "19"})
    missing = [path.name for path in captures if not path.is_file()]
    if missing:
        raise FileNotFoundError("Faltan capturas declaradas: " + ", ".join(missing))
    if not allow_incomplete and (not ui_passed or not unit_passed or backend_passed != backend_total or backend_total == 0 or not captures):
        raise RuntimeError("El informe final exige interfaz APROBADO, JUnit sin fallos, backend aprobado y capturas reales.")
    additional_path = PROOFS / "persistencia_proceso.json"
    if not additional_path.exists():
        additional_path = capture_dir / "persistencia_proceso.json"
    additional = load_json(additional_path, required=False)
    if additional and not allow_incomplete and additional.get("resultado") != "APROBADO":
        raise RuntimeError("La comprobación adicional de persistencia existe pero no está APROBADO.")
    story = [Spacer(1, 19), p("Tu biblioteca.\nTu ritmo. Tu meta.", "ReadingTitle"),
             p("Informe de pruebas de funcionamiento", "ReadingH1"),
             p("Aplicación Android Entre páginas, desarrollada sobre el proyecto original para el Desafío práctico 3 de Desarrollo de Software para Móviles."),
             Spacer(1, 9)]
    summary = [["Nivel de prueba", "Evidencia disponible", "Resultado"],
               ["Lógica y validaciones", f"{unit['tests']} pruebas JUnit ejecutadas", "APROBADO" if unit_passed else "PENDIENTE / FALLÓ"],
               ["Firebase real", f"{backend_total} comprobaciones de Authentication y Realtime Database", f"{backend_passed}/{backend_total} aprobadas"],
               ["Interfaz Android", f"{len(ui.get('verificaciones', []))} verificaciones del recorrido y {len(selected_names)} capturas", "APROBADO" if ui_passed else "PENDIENTE / FALLÓ"]]
    if additional:
        summary.append(["Cierre del proceso", f"Sesión, biblioteca y meta recuperadas; {len(additional.get('capturas', []))} capturas", additional.get("resultado", "PENDIENTE")])
    if build:
        summary.append(["Compilación y revisión", f"APK generado; lint: {build.get('lintErrors', 'no registrado')} errores y {build.get('lintWarnings', 'no registrado')} advertencias", build.get("compilation", "PENDIENTE")])
    story += [table(summary, [124, 274, WIDTH - 398]), Spacer(1, 16), p("Entorno y alcance", "ReadingH2"),
              p(f"Teléfono físico {device.get('fabricante', '')} {device.get('modelo', 'LLY_NX3')} con Android {device.get('android', '16')} (API {device.get('api', 36)}), pantalla {device.get('resolucion', '1080x2412')} píxeles y conexión por ADB. Se conservaron Android Gradle Plugin 9.1.1, compile SDK 37.1, target SDK 36 y min SDK 24. Firebase BoM 34.4.0 se utiliza por compatibilidad con el Kotlin integrado en el proyecto original.", "ReadingSmall"),
              p(f"Firebase: {backend.get('projectId', ui.get('firebaseProjectId', 'No registrado'))}. Las pruebas de servidor usan el servicio remoto real, no datos simulados.", "ReadingSmall"),
              p("Repositorio configurado", "ReadingH2"),
              Paragraph(f'<link href="{REPO}" color="#25675C">{REPO}</link>', styles["ReadingBody"]),
              p("El enlace corresponde al remoto configurado del proyecto. Los cambios de esta entrega son locales y no se afirma que ya estén publicados en GitHub.", "ReadingSmall"),
              p("Criterio de páginas anuales", "ReadingH2"),
              p("Se suman las páginas totales de los libros terminados en el año actual. El avance parcial se muestra en la biblioteca; la app no almacena un historial diario de páginas.", "ReadingSmall"), PageBreak()]

    story += [p("Firebase: datos y acceso privado", "ReadingH1"),
              p("Comprobaciones directas del servidor. Los rechazos esperados cuentan como aprobados cuando el servidor impide la operación inválida o el acceso ajeno.")]
    rows = [["Comprobación", "Resultado observado", "Estado"]]
    for result in backend.get("results", []):
        rows.append([result.get("name", ""), result.get("observed", ""), "Aprobado" if result.get("passed") is True else "Falló"])
    story += [table(rows, [288, 145, WIDTH - 433]), Spacer(1, 12),
              p("Las cuentas temporales de estas pruebas de servidor y sus datos fueron eliminados al finalizar, según el registro de limpieza.", "ReadingSmall")
              if backend.get("cleanup") and all(x.get("dataDeleted") and x.get("accountDeleted") for x in backend["cleanup"])
              else p("El estado de limpieza de las cuentas temporales se conserva en firebase-results.json.", "ReadingSmall"), PageBreak()]

    story += [p("Recorrido de la aplicación", "ReadingH1"),
              p("Las siguientes verificaciones provienen del resultado exportado de la instrumentación Android. Las páginas siguientes muestran las capturas de esa misma ejecución.")]
    for i, check in enumerate(ui.get("verificaciones", []), 1):
        story.append(p(f"{i:02d}. {check}", "ReadingSmall"))
    story += [Spacer(1, 8), p("Pruebas unitarias y límites", "ReadingH2"),
              p(f"JUnit registró {unit['tests']} pruebas, {unit['failures']} fallos, {unit['errors']} errores y {unit['skipped']} omitidas. Los casos cubren validación de cuentas y libros, porcentajes, límites numéricos, selección del año, zonas horarias y empates de estadísticas.", "ReadingSmall"),
              p("Persistencia: alcance de la evidencia", "ReadingH2"),
              p("El recorrido recrea la actividad y vuelve a iniciar sesión para comprobar la recuperación de la cuenta y sus datos. ActivityScenario.recreate() no representa un reinicio del teléfono ni una prueba de apagado del dispositivo.", "ReadingSmall")]
    if additional:
        story.append(p("Prueba adicional de cierre de proceso", "ReadingH2"))
        story.append(p("Se cerró forzadamente el proceso de la aplicación y se volvió a abrir. Esta comprobación es distinta de la recreación de actividad; tampoco constituye un reinicio completo del teléfono.", "ReadingSmall"))
        process_labels = {"uidRestaurado": "Misma cuenta autenticada sin volver a introducir credenciales",
                          "bibliotecaRecuperada": "Libro terminado, avance y estrellas recuperados",
                          "metaRecuperada": "Meta anual y libros restantes recuperados"}
        for key, label in process_labels.items():
            if key in additional:
                story.append(p(f"{label}: {'verificado' if additional[key] is True else 'no verificado'}.", "ReadingSmall"))
        story.append(p(f"Resultado adicional: {additional.get('resultado', 'PENDIENTE')}.", "ReadingSmall"))
    story += [p("Fuentes de evidencia", "ReadingH2"),
              p("output/pruebas/: firebase-results.json, resultado_pruebas.json, persistencia_proceso.json, compilacion.json, dispositivo.json y capturas. Pruebas unitarias: app/build/test-results/testDebugUnitTest/TEST-*.xml. No se incluyen tokens ni contraseñas en este informe.", "ReadingSmall")]

    for start in range(0, len(captures), 2):
        story += [PageBreak(), p(f"Evidencias visuales {start + 1:02d}-{min(start + 2, len(captures)):02d}", "ReadingH1"),
                  p("Capturas reales de la app Android durante las pruebas de funcionamiento.", "ReadingSmall"), Spacer(1, 7)]
        panels = [capture_panel(path, start + idx + 1) for idx, path in enumerate(captures[start:start + 2])]
        if len(panels) == 1:
            panels.append("")
        spread = Table([panels], colWidths=[(WIDTH - 16) / 2, (WIDTH - 16) / 2], hAlign="CENTER")
        spread.setStyle(TableStyle([("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 5),
                                   ("RIGHTPADDING", (0, 0), (-1, -1), 5), ("TOPPADDING", (0, 0), (-1, -1), 4)]))
        story.append(spread)
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    doc = BaseDocTemplate(str(TARGET), pagesize=A4, leftMargin=MARGIN, rightMargin=MARGIN,
                          topMargin=55, bottomMargin=52, title="Entre páginas - Pruebas de funcionamiento",
                          author="", subject="Desafío práctico 3 - DSM441")
    frame = Frame(MARGIN, 52, WIDTH, PAGE_H - 107, leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
    doc.addPageTemplates(PageTemplate(id="report", frames=frame, onPage=background))
    doc.build(story)
    reader = PdfReader(TARGET)
    text = "\n".join(page.extract_text() for page in reader.pages)
    if not text.strip() or REPO not in text:
        raise RuntimeError("La comprobación de texto del PDF no encontró el contenido esperado.")
    print(json.dumps({"pdf": str(TARGET), "pages": len(reader.pages), "unitTests": unit,
                      "firebasePassed": backend_passed, "firebaseTotal": backend_total,
                      "uiResult": ui.get("resultado", "PENDIENTE"), "captures": len(captures)}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--allow-incomplete", action="store_true", help="Permite una vista previa marcada con resultados pendientes; no es una entrega final.")
    args = parser.parse_args()
    create_report(args.allow_incomplete)
