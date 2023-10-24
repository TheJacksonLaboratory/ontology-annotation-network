package org.jax.oan.exception;

/*
	Thrown when our data loader is missing required resources.
 */
public class OntologyAnnotationNetworkDataException extends OntologyAnnotationNetworkException {
	public OntologyAnnotationNetworkDataException() {
		super();
	}

	public OntologyAnnotationNetworkDataException(String message) {
		super(message);
	}

	public OntologyAnnotationNetworkDataException(String message, Exception e) {
		super(message, e);
	}

	public OntologyAnnotationNetworkDataException(String message, Throwable cause) {
		super(message, cause);
	}

	public OntologyAnnotationNetworkDataException(Throwable cause) {
		super(cause);
	}

	protected OntologyAnnotationNetworkDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
