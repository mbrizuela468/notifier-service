using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using NotifierService.Models.WhatsApp;

namespace NotifierService.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class WhatsAppController : BaseController
    {
        private readonly ILogger<WhatsAppController> _logger;
        public WhatsAppController(ILogger<WhatsAppController> pLogger) 
        {
            _logger = pLogger;
        }


        [HttpPost("incoming")]
        public IActionResult Incoming([FromForm] TwilioIncomingMessage incomingMessage)
        {
            //Console.WriteLine($"📩 Nuevo WhatsApp de {incomingMessage.From}: {incomingMessage.Body}");

            _logger.LogDebug($"📩 Nuevo WhatsApp de {incomingMessage.From}: {incomingMessage.Body}");

            // Acá podrías guardar en DB, disparar lógica, etc.
            // Ejemplo: responder automáticamente
            return Content("<Response><Message>Recibido ✅</Message></Response>", "application/xml");
        }
    }
}
