// Servidor del asistente de IA de RecuerdaTec (opcional).
// La clave de la API vive aqui, nunca dentro de la app.
//
//   ANTHROPIC_API_KEY=sk-ant-... node servidor.mjs
//
// Variables: PORT (8787), MODELO (claude-sonnet-5-5). Requiere Node 18 o superior.
import { createServer } from "node:http";

const PORT = Number(process.env.PORT ?? 8787);
const MODELO = process.env.MODELO ?? "claude-sonnet-5-5";
const CLAVE = process.env.ANTHROPIC_API_KEY;

const SISTEMA = `Eres el asistente de RecuerdaTec, una app para personas con Alzheimer o deterioro cognitivo leve.
Habla en espanol, con frases cortas, tono calido y tranquilo. Nunca corrijas con dureza ni discutas.
Usa solo los datos del contexto (agenda, medicacion, familiares). Si no sabes algo, dilo y sugiere
avisar al cuidador. No des consejos medicos ni cambies dosis. Responde en menos de 60 palabras.`;

function cabeceras(res, codigo) {
  res.writeHead(codigo, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "content-type",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
  });
}

async function leerCuerpo(req) {
  let texto = "";
  for await (const trozo of req) {
    texto += trozo;
    if (texto.length > 100_000) throw new Error("Peticion demasiado grande");
  }
  return JSON.parse(texto || "{}");
}

createServer(async (req, res) => {
  if (req.method === "OPTIONS") {
    cabeceras(res, 204);
    return res.end();
  }
  if (req.method !== "POST" || req.url !== "/chat") {
    cabeceras(res, 404);
    return res.end(JSON.stringify({ error: "No encontrado" }));
  }
  if (!CLAVE) {
    cabeceras(res, 500);
    return res.end(JSON.stringify({ error: "Falta ANTHROPIC_API_KEY" }));
  }
  try {
    const { mensaje, contexto } = await leerCuerpo(req);
    const r = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "x-api-key": CLAVE,
        "anthropic-version": "2023-06-01",
      },
      body: JSON.stringify({
        model: MODELO,
        max_tokens: 300,
        system: `${SISTEMA}\n\nContexto actual:\n${contexto ?? ""}`,
        messages: [{ role: "user", content: String(mensaje ?? "") }],
      }),
    });
    const datos = await r.json();
    if (!r.ok) throw new Error(datos?.error?.message ?? "Error de la API");
    const respuesta = (datos.content ?? []).map((b) => b.text ?? "").join("").trim();
    cabeceras(res, 200);
    res.end(JSON.stringify({ respuesta }));
  } catch (e) {
    cabeceras(res, 500);
    res.end(JSON.stringify({ error: String(e.message ?? e) }));
  }
}).listen(PORT, () => console.log(`Asistente de RecuerdaTec en http://localhost:${PORT}`));
