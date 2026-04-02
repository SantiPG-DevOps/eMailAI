# 📧 eMail-IA

Cliente de correo inteligente de escritorio con filtrado avanzado de **spam** y **phishing** mediante IA local, priorizando privacidad, seguridad y control del usuario.

---

## ✨ Características

- 🧠 Clasificación automática de correos en legítimos, spam y phishing, aprendiendo de las acciones del usuario.  
- 📚 Modelos de aprendizaje entrenados por cuenta, adaptados a los hábitos de cada usuario.  
- 🔍 Explicaciones básicas de cada clasificación (palabras clave, dominio sospechoso, similitud con correos marcados, etc.).  
- 📂 Cliente de correo completo: lectura, envío, respuesta, reenvío, archivado y eliminación.  
- 🔒 Procesamiento totalmente local, sin enviar mensajes ni datos de entrenamiento a la nube.  

---

## 🛡️ Privacidad y seguridad

- Base de datos local (SQLite/H2) cifrada para credenciales y datos de entrenamiento.  
- Conexiones seguras IMAP/SMTP mediante TLS con los servidores de correo.  
- Separación entre datos sensibles de autenticación y datos usados por la IA.  
- Posibilidad de eliminar los datos de entrenamiento cuando el usuario lo desee.  

---

## 🧱 Arquitectura

eMail-IA sigue una arquitectura en capas para facilitar mantenimiento y futuras ampliaciones:

- **Capa de presentación:** interfaz de usuario en JavaFX (login, bandejas, lectura, redacción, configuración).  
- **Lógica de negocio:** servicios de gestión de cuentas, sincronización IMAP/SMTP y coordinación del filtrado inteligente.  
- **Acceso a datos:** capa de persistencia sobre base de datos local cifrada (cuentas, modelos, metadatos, ejemplos de entrenamiento).  
- **Módulo de IA:** integración con Weka para entrenar y aplicar modelos de clasificación por cuenta, con explicabilidad básica.  
- **Módulo de seguridad:** cifrado de datos en reposo y configuración de conexiones seguras con los servidores de correo.  

---

## 🛠️ Tecnologías

- **Lenguaje:** Java  
- **UI:** JavaFX  
- **Correo:** Jakarta Mail (IMAP, SMTP, HTML, adjuntos, TLS).  
- **IA:** Weka (clasificación supervisada para spam/phishing).  
- **Base de datos:** SQLite o H2 en local, con cifrado.  
- **Control de versiones:** Git + GitHub.  

---

## 🎯 Objetivo del proyecto

eMail-IA busca mejorar la experiencia de uso del correo electrónico combinando un cliente de escritorio multiplataforma con un sistema de filtrado inteligente que se adapta a cada usuario.  
El objetivo es reducir la saturación de spam, mitigar riesgos de phishing y ofrecer transparencia en las decisiones de la IA, manteniendo siempre el control y la privacidad en el equipo del usuario.

---

## 👤 Autor

**Santiago Pérez Gómez**  
Proyecto Final de Ciclo — Aplicación inteligente para gestión avanzada de correo.

📍Málaga, España @SantiPG-Dev
